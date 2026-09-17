package org.fusesource.camel.component.sap.springboot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.spring.boot.ComponentConfigurationPropertiesCommon;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "camel.component.sap")
public class SapConnectionConfiguration extends ComponentConfigurationPropertiesCommon {

	/**
	 * To use the shared configuration
	 */
	 private SapConnectionConfigurationNestedConfiguration configuration;

	/**
	 * Whether the component should resolve property placeholders on itself when
	 * starting. Only properties which are of String type can use property
	 * placeholders.
	 */
	private Boolean resolvePropertyPlaceholders = true;

	public SapConnectionConfigurationNestedConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(SapConnectionConfigurationNestedConfiguration configuration) {
		this.configuration = configuration;
	}

	public Boolean getResolvePropertyPlaceholders() {
		return resolvePropertyPlaceholders;
	}

	public void setResolvePropertyPlaceholders(Boolean resolvePropertyPlaceholders) {
		this.resolvePropertyPlaceholders = resolvePropertyPlaceholders;
	}

	@Configuration
	public static class SapConnectionConfigurationNestedConfiguration {
		private Map<String, DestinationData> destinationDataStore = new HashMap<String, DestinationData>();
		private Map<String, ServerData> serverDataStore = new HashMap<String, ServerData>();
		private Map<String, RepositoryData> repositoryDataStore = new HashMap<String, RepositoryData>();

		public Map<String, DestinationData> getDestinationDataStore() {
			return destinationDataStore;
		}

		public void setDestinationDataStore(Map<String, DestinationData> destinationDataStore) {
			this.destinationDataStore = destinationDataStore;
		}

		public Map<String, ServerData> getServerDataStore() {
			return serverDataStore;
		}

		public void setServerDataStore(Map<String, ServerData> serverDataStore) {
			this.serverDataStore = serverDataStore;
		}

		public Map<String, RepositoryData> getRepositoryDataStore() {
			return repositoryDataStore;
		}

		public void setRepositoryDataStore(Map<String, RepositoryData> repositoryDataStore) {
			this.repositoryDataStore = repositoryDataStore;
		}

		public static class DestinationData {
			private String aliasUser;
			private String ashost;
			private String authType;
			private String client;
			private String codepage;
			private String cpicTrace;
			private String denyInitialPassword;
			private String expirationPeriod;
			private String expirationTime;
			private String getsso2;
			private String group;
			private String gwhost;
			private String gwserv;
			private String lang;
			private String lcheck;
			private String maxGetTime;
			private String mshost;
			private String msserv;
			private String mysapsso2;
			private String passwd;
			private String password;
			private String pcs;
			private String peakLimit;
			private String pingOnCreate;
			private String poolCapacity;
			private String r3name;
			private String repositoryDest;
			private String repositoryPasswd;
			private String repositoryRoundtripOptimization;
			private String repositorySnc;
			private String repositoryUser;
			private String saprouter;
			private String sncLibrary;
			private String sncMode;
			private String sncMyname;
			private String sncPartnername;
			private String sncQop;
			private String sysnr;
			private String tphost;
			private String tpname;
			private String trace;
			private String type;
			private String userName;
			private String user;
			private String userId;
			private String useSapgui;
			private String x509cert;

			public String getAliasUser() {
				return aliasUser;
			}

			public void setAliasUser(String aliasUser) {
				this.aliasUser = aliasUser;
			}

			public String getAshost() {
				return ashost;
			}

			public void setAshost(String ashost) {
				this.ashost = ashost;
			}

			public String getAuthType() {
				return authType;
			}

			public void setAuthType(String authType) {
				this.authType = authType;
			}

			public String getClient() {
				return client;
			}

			public void setClient(String client) {
				this.client = client;
			}

			public String getCodepage() {
				return codepage;
			}

			public void setCodepage(String codepage) {
				this.codepage = codepage;
			}

			public String getCpicTrace() {
				return cpicTrace;
			}

			public void setCpicTrace(String cpicTrace) {
				this.cpicTrace = cpicTrace;
			}

			public String getDenyInitialPassword() {
				return denyInitialPassword;
			}

			public void setDenyInitialPassword(String denyInitialPassword) {
				this.denyInitialPassword = denyInitialPassword;
			}

			public String getExpirationPeriod() {
				return expirationPeriod;
			}

			public void setExpirationPeriod(String expirationPeriod) {
				this.expirationPeriod = expirationPeriod;
			}

			public String getExpirationTime() {
				return expirationTime;
			}

			public void setExpirationTime(String expirationTime) {
				this.expirationTime = expirationTime;
			}

			public String getGetsso2() {
				return getsso2;
			}

			public void setGetsso2(String getsso2) {
				this.getsso2 = getsso2;
			}

			public String getGroup() {
				return group;
			}

			public void setGroup(String group) {
				this.group = group;
			}

			public String getGwhost() {
				return gwhost;
			}

			public void setGwhost(String gwhost) {
				this.gwhost = gwhost;
			}

			public String getGwserv() {
				return gwserv;
			}

			public void setGwserv(String gwserv) {
				this.gwserv = gwserv;
			}

			public String getLang() {
				return lang;
			}

			public void setLang(String lang) {
				this.lang = lang;
			}

			public String getLcheck() {
				return lcheck;
			}

			public void setLcheck(String lcheck) {
				this.lcheck = lcheck;
			}

			public String getMaxGetTime() {
				return maxGetTime;
			}

			public void setMaxGetTime(String maxGetTime) {
				this.maxGetTime = maxGetTime;
			}

			public String getMshost() {
				return mshost;
			}

			public void setMshost(String mshost) {
				this.mshost = mshost;
			}

			public String getMsserv() {
				return msserv;
			}

			public void setMsserv(String msserv) {
				this.msserv = msserv;
			}

			public String getMysapsso2() {
				return mysapsso2;
			}

			public void setMysapsso2(String mysapsso2) {
				this.mysapsso2 = mysapsso2;
			}

			public String getPasswd() {
				return passwd;
			}

			public void setPasswd(String passwd) {
				this.passwd = passwd;
			}

			public String getPassword() {
				return password;
			}

			public void setPassword(String password) {
				this.password = password;
			}

			public String getPcs() {
				return pcs;
			}

			public void setPcs(String pcs) {
				this.pcs = pcs;
			}

			public String getPeakLimit() {
				return peakLimit;
			}

			public void setPeakLimit(String peakLimit) {
				this.peakLimit = peakLimit;
			}

			public String getPingOnCreate() {
				return pingOnCreate;
			}

			public void setPingOnCreate(String pingOnCreate) {
				this.pingOnCreate = pingOnCreate;
			}

			public String getPoolCapacity() {
				return poolCapacity;
			}

			public void setPoolCapacity(String poolCapacity) {
				this.poolCapacity = poolCapacity;
			}

			public String getR3name() {
				return r3name;
			}

			public void setR3name(String r3name) {
				this.r3name = r3name;
			}

			public String getRepositoryDest() {
				return repositoryDest;
			}

			public void setRepositoryDest(String repositoryDest) {
				this.repositoryDest = repositoryDest;
			}

			public String getRepositoryPasswd() {
				return repositoryPasswd;
			}

			public void setRepositoryPasswd(String repositoryPasswd) {
				this.repositoryPasswd = repositoryPasswd;
			}

			public String getRepositoryRoundtripOptimization() {
				return repositoryRoundtripOptimization;
			}

			public void setRepositoryRoundtripOptimization(String repositoryRoundtripOptimization) {
				this.repositoryRoundtripOptimization = repositoryRoundtripOptimization;
			}

			public String getRepositorySnc() {
				return repositorySnc;
			}

			public void setRepositorySnc(String repositorySnc) {
				this.repositorySnc = repositorySnc;
			}

			public String getRepositoryUser() {
				return repositoryUser;
			}

			public void setRepositoryUser(String repositoryUser) {
				this.repositoryUser = repositoryUser;
			}

			public String getSaprouter() {
				return saprouter;
			}

			public void setSaprouter(String saprouter) {
				this.saprouter = saprouter;
			}

			public String getSncLibrary() {
				return sncLibrary;
			}

			public void setSncLibrary(String sncLibrary) {
				this.sncLibrary = sncLibrary;
			}

			public String getSncMode() {
				return sncMode;
			}

			public void setSncMode(String sncMode) {
				this.sncMode = sncMode;
			}

			public String getSncMyname() {
				return sncMyname;
			}

			public void setSncMyname(String sncMyname) {
				this.sncMyname = sncMyname;
			}

			public String getSncPartnername() {
				return sncPartnername;
			}

			public void setSncPartnername(String sncPartnername) {
				this.sncPartnername = sncPartnername;
			}

			public String getSncQop() {
				return sncQop;
			}

			public void setSncQop(String sncQop) {
				this.sncQop = sncQop;
			}

			public String getSysnr() {
				return sysnr;
			}

			public void setSysnr(String sysnr) {
				this.sysnr = sysnr;
			}

			public String getTphost() {
				return tphost;
			}

			public void setTphost(String tphost) {
				this.tphost = tphost;
			}

			public String getTpname() {
				return tpname;
			}

			public void setTpname(String tpname) {
				this.tpname = tpname;
			}

			public String getTrace() {
				return trace;
			}

			public void setTrace(String trace) {
				this.trace = trace;
			}

			public String getType() {
				return type;
			}

			public void setType(String type) {
				this.type = type;
			}

			public String getUserName() {
				return userName;
			}

			public void setUserName(String userName) {
				this.userName = userName;
			}

			public String getUser() {
				return user;
			}

			public void setUser(String user) {
				this.user = user;
			}

			public String getUserId() {
				return userId;
			}

			public void setUserId(String userId) {
				this.userId = userId;
			}

			public String getUseSapgui() {
				return useSapgui;
			}

			public void setUseSapgui(String useSapgui) {
				this.useSapgui = useSapgui;
			}

			public String getX509cert() {
				return x509cert;
			}

			public void setX509cert(String x509cert) {
				this.x509cert = x509cert;
			}

		}

		public static class ServerData {
			private String gwhost;
			private String gwserv;
			private String progid;
			private String connectionCount;
			private String saprouter;
			private String maxStartUpDelay;
			private String repositoryDestination;
			private String repositoryMap;
			private String trace;
			private String workerThreadCount;
			private String workerThreadMinCount;
			private String sncMode;
			private String sncQop;
			private String sncMyname;
			private String sncLib;

			public String getGwhost() {
				return gwhost;
			}

			public void setGwhost(String gwhost) {
				this.gwhost = gwhost;
			}

			public String getGwserv() {
				return gwserv;
			}

			public void setGwserv(String gwserv) {
				this.gwserv = gwserv;
			}

			public String getProgid() {
				return progid;
			}

			public void setProgid(String progid) {
				this.progid = progid;
			}

			public String getConnectionCount() {
				return connectionCount;
			}

			public void setConnectionCount(String connectionCount) {
				this.connectionCount = connectionCount;
			}

			public String getSaprouter() {
				return saprouter;
			}

			public void setSaprouter(String saprouter) {
				this.saprouter = saprouter;
			}

			public String getMaxStartUpDelay() {
				return maxStartUpDelay;
			}

			public void setMaxStartUpDelay(String maxStartUpDelay) {
				this.maxStartUpDelay = maxStartUpDelay;
			}

			public String getRepositoryDestination() {
				return repositoryDestination;
			}

			public void setRepositoryDestination(String repositoryDestination) {
				this.repositoryDestination = repositoryDestination;
			}

			public String getRepositoryMap() {
				return repositoryMap;
			}

			public void setRepositoryMap(String repositoryMap) {
				this.repositoryMap = repositoryMap;
			}

			public String getTrace() {
				return trace;
			}

			public void setTrace(String trace) {
				this.trace = trace;
			}

			public String getWorkerThreadCount() {
				return workerThreadCount;
			}

			public void setWorkerThreadCount(String workerThreadCount) {
				this.workerThreadCount = workerThreadCount;
			}

			public String getWorkerThreadMinCount() {
				return workerThreadMinCount;
			}

			public void setWorkerThreadMinCount(String workerThreadMinCount) {
				this.workerThreadMinCount = workerThreadMinCount;
			}

			public String getSncMode() {
				return sncMode;
			}

			public void setSncMode(String sncMode) {
				this.sncMode = sncMode;
			}

			public String getSncQop() {
				return sncQop;
			}

			public void setSncQop(String sncQop) {
				this.sncQop = sncQop;
			}

			public String getSncMyname() {
				return sncMyname;
			}

			public void setSncMyname(String sncMyname) {
				this.sncMyname = sncMyname;
			}

			public String getSncLib() {
				return sncLib;
			}

			public void setSncLib(String sncLib) {
				this.sncLib = sncLib;
			}
		}
		
		 public static class RepositoryData {
			 
			 private Map<String, FunctionTemplate> functionTemplates;

			public Map<String, FunctionTemplate> getFunctionTemplates() {
				return functionTemplates;
			}

			public void setFunctionTemplates(Map<String, FunctionTemplate> functionTemplates) {
				this.functionTemplates = functionTemplates;
			}
				
		 }
		 
		public static class FunctionTemplate {

			private List<ListFieldMetaData> importParameterList;
			private List<ListFieldMetaData> exportParameterList;
			private List<ListFieldMetaData> changingParameterList;
			private List<ListFieldMetaData> tableParameterList;
			private List<AbapException> exceptionList;

			public List<ListFieldMetaData> getImportParameterList() {
				return importParameterList;
			}

			public void setImportParameterList(List<ListFieldMetaData> importParameterList) {
				this.importParameterList = importParameterList;
			}

			public List<ListFieldMetaData> getExportParameterList() {
				return exportParameterList;
			}

			public void setExportParameterList(List<ListFieldMetaData> exportParameterList) {
				this.exportParameterList = exportParameterList;
			}

			public List<ListFieldMetaData> getChangingParameterList() {
				return changingParameterList;
			}

			public void setChangingParameterList(List<ListFieldMetaData> changingParameterList) {
				this.changingParameterList = changingParameterList;
			}

			public List<ListFieldMetaData> getTableParameterList() {
				return tableParameterList;
			}

			public void setTableParameterList(List<ListFieldMetaData> tableParameterList) {
				this.tableParameterList = tableParameterList;
			}

			public List<AbapException> getExceptionList() {
				return exceptionList;
			}

			public void setExceptionList(List<AbapException> exceptionList) {
				this.exceptionList = exceptionList;
			}

		}
		 
		public static class AbapException {

			private String key;
			private String message;

			public String getKey() {
				return key;
			}

			public void setKey(String key) {
				this.key = key;
			}

			public String getMessage() {
				return message;
			}

			public void setMessage(String message) {
				this.message = message;
			}

		}

		public static class ListFieldMetaData {

			private String name;
			private String description;
			private String type;
			private boolean export;
			private boolean import_;
			private boolean changing;
			private boolean exception;
			private boolean optional;
			private String defaults;
			private int decimals;
			private int byteLength;
			private int unicodeByteLength;

			private RecordMetaData recordMetaData;

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getDescription() {
				return description;
			}

			public void setDescription(String description) {
				this.description = description;
			}

			public String getType() {
				return type;
			}

			public void setType(String type) {
				this.type = type;
			}

			public boolean isExport() {
				return export;
			}

			public void setExport(boolean export) {
				this.export = export;
			}

			public boolean isImport() {
				return import_;
			}

			public void setImport(boolean import_) {
				this.import_ = import_;
			}

			public boolean isChanging() {
				return changing;
			}

			public void setChanging(boolean changing) {
				this.changing = changing;
			}

			public boolean isException() {
				return exception;
			}

			public void setException(boolean exception) {
				this.exception = exception;
			}

			public boolean isOptional() {
				return optional;
			}

			public void setOptional(boolean optional) {
				this.optional = optional;
			}

			public String getDefaults() {
				return defaults;
			}

			public void setDefaults(String defaults) {
				this.defaults = defaults;
			}

			public int getDecimals() {
				return decimals;
			}

			public void setDecimals(int decimals) {
				this.decimals = decimals;
			}

			public int getByteLength() {
				return byteLength;
			}

			public void setByteLength(int byteLength) {
				this.byteLength = byteLength;
			}

			public int getUnicodeByteLength() {
				return unicodeByteLength;
			}

			public void setUnicodeByteLength(int unicodeByteLength) {
				this.unicodeByteLength = unicodeByteLength;
			}

			public RecordMetaData getRecordMetaData() {
				return recordMetaData;
			}

			public void setRecordMetaData(RecordMetaData recordMetaData) {
				this.recordMetaData = recordMetaData;
			}

		}
		 
		public static class FieldMetaData {

			private String name;
			private String description;
			private String type;
			private int decimals;
			private int byteLength;
			private int byteOffset;
			private int unicodeByteLength;
			private int unicodeByteOffset;

			private RecordMetaData recordMetaData;

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getDescription() {
				return description;
			}

			public void setDescription(String description) {
				this.description = description;
			}

			public String getType() {
				return type;
			}

			public void setType(String type) {
				this.type = type;
			}

			public int getDecimals() {
				return decimals;
			}

			public void setDecimals(int decimals) {
				this.decimals = decimals;
			}

			public int getByteLength() {
				return byteLength;
			}

			public void setByteLength(int byteLength) {
				this.byteLength = byteLength;
			}

			public int getByteOffset() {
				return byteOffset;
			}

			public void setByteOffset(int byteOffset) {
				this.byteOffset = byteOffset;
			}

			public int getUnicodeByteLength() {
				return unicodeByteLength;
			}

			public void setUnicodeByteLength(int unicodeByteLength) {
				this.unicodeByteLength = unicodeByteLength;
			}

			public int getUnicodeByteOffset() {
				return unicodeByteOffset;
			}

			public void setUnicodeByteOffset(int unicodeByteOffset) {
				this.unicodeByteOffset = unicodeByteOffset;
			}

			public RecordMetaData getRecordMetaData() {
				return recordMetaData;
			}

			public void setRecordMetaData(RecordMetaData recordMetaData) {
				this.recordMetaData = recordMetaData;
			}

		}
		 
		public static class RecordMetaData {

			private String name;
			private List<FieldMetaData> recordFieldMetaData;

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public List<FieldMetaData> getRecordFieldMetaData() {
				return recordFieldMetaData;
			}

			public void setRecordFieldMetaData(List<FieldMetaData> recordFieldMetaData) {
				this.recordFieldMetaData = recordFieldMetaData;
			}

		}

	}
	
}
