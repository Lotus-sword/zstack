package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.PolicyVO;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/identities/policies/{policyUuid}/roles/{roleUuid}", method = HttpMethod.POST, responseClass = APIAttachPolicyToRoleEvent.class)
public class APIAttachPolicyToRoleMsg extends APIMessage {
    @APIParam(resourceType = RoleVO.class, checkAccount = true, operationTarget = true)
    private String roleUuid;
    @APIParam(resourceType = PolicyVO.class, checkAccount = true, operationTarget = true)
    private String policyUuid;

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public void setPolicyUuid(String policyUuid) {
        this.policyUuid = policyUuid;
    }
}
