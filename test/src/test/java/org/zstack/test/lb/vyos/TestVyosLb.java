package org.zstack.test.lb.vyos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipNetworkServicesRefVO;
import org.zstack.network.service.vip.VipNetworkServicesRefVO_;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterVmMsg;
import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterVmReply;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.LbTO;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.RefreshLbCmd;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * @author frank
 *         <p>
 *         1. create a lb
 *         <p>
 *         confirm lb are created successfully
 *         <p>
 *         2. delete the lb
 *         <p>
 *         confirm all related resources deleted
 *         <p>
 *         3. create a new lb
 *         <p>
 *         confirm the vip is locked
 *         <p>
 *         4. delete the new lb
 *         <p>
 *         confirm the vip is unlocked
 *         <p>
 *         5. delete vip on lb
 *         <p>
 *         confirm the lb is deleted
 *         <p>
 *         6. query the vr
 *         <p>
 *         confirm the result is right
 */
public class TestVyosLb {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    ApplianceVmSimulatorConfig aconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/lb/TestVyosLb.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("lb.xml");
        deployer.addSpringConfig("vyos.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        aconfig = loader.getComponent(ApplianceVmSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        LoadBalancerInventory lb = deployer.loadBalancers.get("lb");
        LoadBalancerVO lbvo = dbf.findByUuid(lb.getUuid(), LoadBalancerVO.class);
        Assert.assertNotNull(lbvo);
        Assert.assertNotNull(lbvo.getProviderType());
        Assert.assertFalse(lbvo.getListeners().isEmpty());
        Assert.assertFalse(lbvo.getListeners().iterator().next().getVmNicRefs().isEmpty());

        VipVO vip = dbf.findByUuid(lbvo.getVipUuid(), VipVO.class);
        Assert.assertNotNull(vip);
        Assert.assertEquals(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE, vip.getServiceProvider());
        Assert.assertFalse(vconfig.vips.isEmpty());
        String ref = Q.New(VipNetworkServicesRefVO.class).select(VipNetworkServicesRefVO_.serviceType).eq(VipNetworkServicesRefVO_.uuid,lbvo.getUuid()).find();
        Assert.assertEquals(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING, ref);

        Assert.assertFalse(vconfig.refreshLbCmds.isEmpty());
        RefreshLbCmd cmd = vconfig.refreshLbCmds.get(0);
        Assert.assertFalse(cmd.getLbs().isEmpty());
        LbTO to = cmd.getLbs().get(0);
        LoadBalancerListenerVO l = lbvo.getListeners().iterator().next();
        Assert.assertEquals(l.getProtocol(), to.getMode());
        Assert.assertEquals(l.getInstancePort(), to.getInstancePort());
        Assert.assertEquals(l.getLoadBalancerPort(), to.getLoadBalancerPort());

        Assert.assertEquals(vip.getIp(), to.getVip());

        L3NetworkInventory gnw = deployer.l3Networks.get("GuestNetwork");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.findNic(gnw.getUuid());
        Assert.assertFalse(to.getNicIps().isEmpty());
        String nicIp = to.getNicIps().get(0);
        Assert.assertEquals(nic.getIp(), nicIp);

        api.deleteLoadBalancer(lb.getUuid(), null);
        Assert.assertEquals(0, dbf.count(LoadBalancerVO.class));
        Assert.assertEquals(0, dbf.count(LoadBalancerListenerVO.class));
        Assert.assertEquals(0, dbf.count(LoadBalancerListenerVmNicRefVO.class));
        Assert.assertFalse(dbf.isExist(lb.getUuid(), VipNetworkServicesRefVO.class));

        L3NetworkInventory pubNw = deployer.l3Networks.get("PublicNetwork");
        VipInventory vip1 = api.acquireIp(pubNw.getUuid());
        LoadBalancerInventory lb2 = api.createLoadBalancer("lb2", vip1.getUuid(), null, null);
        ref = Q.New(VipNetworkServicesRefVO.class).select(VipNetworkServicesRefVO_.serviceType).eq(VipNetworkServicesRefVO_.uuid,lb2.getUuid()).find();
        Assert.assertEquals(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING, ref);

        api.deleteLoadBalancer(lb2.getUuid(), null);
        Assert.assertFalse(dbf.isExist(lb2.getUuid(), VipNetworkServicesRefVO.class));
        Assert.assertFalse(dbf.isExist(lb2.getUuid(), LoadBalancerVO.class));

        vip1 = api.acquireIp(pubNw.getUuid());
        LoadBalancerInventory lb3 = api.createLoadBalancer("lb3", vip1.getUuid(), null, null);
        api.releaseIp(lb3.getVipUuid());
        Assert.assertFalse(dbf.isExist(lb3.getUuid(), LoadBalancerVO.class));

        APIQueryVirtualRouterVmMsg msg = new APIQueryVirtualRouterVmMsg();
        msg.addQueryCondition("__systemTag__", QueryOp.EQ, "role::LoadBalancer");
        APIQueryVirtualRouterVmReply r = api.query(msg, APIQueryVirtualRouterVmReply.class);
        Assert.assertEquals(1, r.getInventories().size());
    }
}
