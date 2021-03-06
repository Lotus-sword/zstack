package org.zstack.storage.backup.sftp;

import org.zstack.configuration.BusinessProperties;
import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

/**
 */

@GlobalPropertyDefinition
public class SftpBackupStorageGlobalProperty {
    @GlobalProperty(name="SftpBackupStorage.agentPackageName", defaultValue = "sftpbackupstorage-3.8.0.tar.gz")
    public static String AGENT_PACKAGE_NAME;
    @GlobalProperty(name="SftpBackupStorage.agentPort", defaultValue = "7171")
    public static int AGENT_PORT;
    @GlobalProperty(name="SftpBackupStorage.agentUrlScheme", defaultValue = "http")
    public static String AGENT_URL_SCHEME;
    @GlobalProperty(name="SftpBackupStorage.agentUrlRootPath", defaultValue = "")
    public static String AGENT_URL_ROOT_PATH;
    @GlobalProperty(name="SftpBackupStorage.DownloadCmd.timeout", defaultValue = "7200")
    public static int DOWNLOAD_CMD_TIMEOUT;

    public static String IPTABLES_KEY = "SftpBackupStorage.iptables.rule";
    public static List<String> IPTABLES_RULES = BusinessProperties.getPropertiesAsList(IPTABLES_KEY);
}
