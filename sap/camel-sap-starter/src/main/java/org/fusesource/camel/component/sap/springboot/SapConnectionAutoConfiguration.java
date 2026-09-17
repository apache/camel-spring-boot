package org.fusesource.camel.component.sap.springboot;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.spring.boot.ComponentConfigurationProperties;
import org.apache.camel.spring.boot.util.CamelPropertiesHelper;
import org.apache.camel.spring.boot.util.ConditionalOnCamelContextAndAutoConfigurationBeans;
import org.apache.camel.spring.boot.util.GroupCondition;
import org.apache.camel.spi.BeanIntrospection;
import org.apache.camel.support.PluginHelper;
import org.fusesource.camel.component.sap.model.rfc.DataType;
import org.fusesource.camel.component.sap.model.rfc.impl.AbapExceptionImpl;
import org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl;
import org.fusesource.camel.component.sap.model.rfc.impl.FieldMetaDataImpl;
import org.fusesource.camel.component.sap.model.rfc.impl.FunctionTemplateImpl;
import org.fusesource.camel.component.sap.model.rfc.impl.ListFieldMetaDataImpl;
import org.fusesource.camel.component.sap.model.rfc.impl.RecordMetaDataImpl;
import org.fusesource.camel.component.sap.model.rfc.impl.RepositoryDataImpl;
import org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl;
import org.fusesource.camel.component.sap.springboot.SapConnectionConfiguration.SapConnectionConfigurationNestedConfiguration.AbapException;
import org.fusesource.camel.component.sap.springboot.SapConnectionConfiguration.SapConnectionConfigurationNestedConfiguration.DestinationData;
import org.fusesource.camel.component.sap.springboot.SapConnectionConfiguration.SapConnectionConfigurationNestedConfiguration.FieldMetaData;
import org.fusesource.camel.component.sap.springboot.SapConnectionConfiguration.SapConnectionConfigurationNestedConfiguration.FunctionTemplate;
import org.fusesource.camel.component.sap.springboot.SapConnectionConfiguration.SapConnectionConfigurationNestedConfiguration.ListFieldMetaData;
import org.fusesource.camel.component.sap.springboot.SapConnectionConfiguration.SapConnectionConfigurationNestedConfiguration.RecordMetaData;
import org.fusesource.camel.component.sap.springboot.SapConnectionConfiguration.SapConnectionConfigurationNestedConfiguration.RepositoryData;
import org.fusesource.camel.component.sap.springboot.SapConnectionConfiguration.SapConnectionConfigurationNestedConfiguration.ServerData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional({ConditionalOnCamelContextAndAutoConfigurationBeans.class,
	SapConnectionAutoConfiguration.GroupConditions.class})
@AutoConfigureAfter(CamelAutoConfiguration.class)
@AutoConfigureBefore({
	org.fusesource.camel.component.sap.SapSynchronousRfcDestinationComponent.class,
	org.fusesource.camel.component.sap.SapTransactionalRfcDestinationComponent.class,
	org.fusesource.camel.component.sap.SapQueuedRfcDestinationComponent.class,
	org.fusesource.camel.component.sap.SapSynchronousRfcServerComponent.class,
	org.fusesource.camel.component.sap.SapTransactionalRfcServerComponent.class,
	org.fusesource.camel.component.sap.SapTransactionalIDocDestinationComponent.class,
	org.fusesource.camel.component.sap.SapTransactionalIDocListDestinationComponent.class,
	org.fusesource.camel.component.sap.SapQueuedIDocDestinationComponent.class,
	org.fusesource.camel.component.sap.SapQueuedIDocListDestinationComponent.class,
	org.fusesource.camel.component.sap.SapTransactionalIDocListServerComponent.class
	})
@EnableConfigurationProperties({ComponentConfigurationProperties.class,
        SapConnectionConfiguration.class})
public class SapConnectionAutoConfiguration {
	@Autowired
    private CamelContext camelContext;
	@Autowired
	private SapConnectionConfiguration configuration;

    static class GroupConditions extends GroupCondition {
        public GroupConditions() {
            super("camel.component", "camel.component.sap");
        }
    }
    
    @Bean(name = "sap-connection-configuration")
    @ConditionalOnMissingBean(org.fusesource.camel.component.sap.SapConnectionConfiguration.class)
	public org.fusesource.camel.component.sap.SapConnectionConfiguration configureSapConnectionConfiguration() throws Exception {
		org.fusesource.camel.component.sap.SapConnectionConfiguration configuration = new org.fusesource.camel.component.sap.SapConnectionConfiguration();

		if (this.configuration.getConfiguration() != null) {
			Map<String, DestinationData> destinationsData = this.configuration.getConfiguration()
					.getDestinationDataStore();
			if (destinationsData != null) {
				for (Entry<String, DestinationData> entry : destinationsData.entrySet()) {
					DestinationDataImpl destinationData = new DestinationDataImpl();

					Map<String, Object> parameters = new HashMap<String, Object>();

					BeanIntrospection bi = PluginHelper.getBeanIntrospection(camelContext);
					bi.getProperties(entry.getValue(), parameters, null, false);
					CamelPropertiesHelper.setCamelProperties(camelContext, destinationData, parameters, false);
	
					configuration.getDestinationDataStore().put(entry.getKey(), destinationData);
				}
			}

			Map<String, ServerData> serversData = this.configuration.getConfiguration().getServerDataStore();
			if (serversData != null) {
				for (Entry<String, ServerData> entry : serversData.entrySet()) {
					ServerDataImpl serverData = new ServerDataImpl();

					Map<String, Object> parameters = new HashMap<String, Object>();
					BeanIntrospection bi = PluginHelper.getBeanIntrospection(camelContext);
					bi.getProperties(entry.getValue(), parameters, null, false);
					CamelPropertiesHelper.setCamelProperties(camelContext, serverData, parameters, false);

					configuration.getServerDataStore().put(entry.getKey(), serverData);
				}
			}

			Map<String, RepositoryData> repositoriesData = this.configuration.getConfiguration().getRepositoryDataStore();
			if (repositoriesData != null) {
				for (Entry<String, RepositoryData> entry : repositoriesData.entrySet()) {
					RepositoryDataImpl respositoryData = new RepositoryDataImpl();
					
					Map<String, FunctionTemplate> functionTemplates = entry.getValue().getFunctionTemplates();
					for (Entry<String,FunctionTemplate> functionTemplateEntry : functionTemplates.entrySet() ) {
						FunctionTemplateImpl functionTemplate = new FunctionTemplateImpl();

						if (functionTemplateEntry.getValue().getImportParameterList() != null) {
							for (ListFieldMetaData importParameterEntry : functionTemplateEntry.getValue()
									.getImportParameterList()) {

								ListFieldMetaDataImpl importParameter = configureListFieldMetaData(
										importParameterEntry);

								functionTemplate.getImportParameterList().add(importParameter);
							}
						}

						if (functionTemplateEntry.getValue().getExportParameterList() != null) {
							for (ListFieldMetaData exportParameterEntry : functionTemplateEntry.getValue()
									.getExportParameterList()) {

								ListFieldMetaDataImpl exportParameter = configureListFieldMetaData(
										exportParameterEntry);

								functionTemplate.getExportParameterList().add(exportParameter);
							}
						}

						if (functionTemplateEntry.getValue().getChangingParameterList() != null) {
							for (ListFieldMetaData changingParameterEntry : functionTemplateEntry.getValue()
									.getChangingParameterList()) {

								ListFieldMetaDataImpl changingParameter = configureListFieldMetaData(
										changingParameterEntry);

								functionTemplate.getChangingParameterList().add(changingParameter);
							}
						}

						if (functionTemplateEntry.getValue().getTableParameterList() != null) {
							for (ListFieldMetaData tableParameterEntry : functionTemplateEntry.getValue()
									.getTableParameterList()) {

								ListFieldMetaDataImpl tableParameter = configureListFieldMetaData(
										tableParameterEntry);

								functionTemplate.getTableParameterList().add(tableParameter);
							}
						}
						

						if (functionTemplateEntry.getValue().getExceptionList() != null) {
							for (AbapException abapExceptionEntry : functionTemplateEntry.getValue()
									.getExceptionList()) {
								
								AbapExceptionImpl abapException = new AbapExceptionImpl();
								abapException.setKey(abapExceptionEntry.getKey());
								abapException.setMessage(abapExceptionEntry.getMessage());
								
								functionTemplate.getExceptionList().add(abapException);
							}
						}
						

						respositoryData.getFunctionTemplates().put(functionTemplateEntry.getKey(), functionTemplate);
					}

					configuration.getRepositoryDataStore().put(entry.getKey(), respositoryData);
				}
			}
		}
		return configuration;
	}
    
    private ListFieldMetaDataImpl configureListFieldMetaData(ListFieldMetaData listFieldMetaData) {
    	
    		ListFieldMetaDataImpl configuration = new ListFieldMetaDataImpl();
    	
		configuration.setName(listFieldMetaData.getName());
		configuration.setDescription(listFieldMetaData.getDescription());
		configuration.setType(DataType.getByName(listFieldMetaData.getType()));
		configuration.setOptional(listFieldMetaData.isOptional());
		configuration.setImport(listFieldMetaData.isImport());
		configuration.setExport(listFieldMetaData.isExport());
		configuration.setChanging(listFieldMetaData.isChanging());
		configuration.setException(listFieldMetaData.isException());
		configuration.setDefaults(listFieldMetaData.getDefaults());
		configuration.setDecimals(listFieldMetaData.getDecimals());
		configuration.setByteLength(listFieldMetaData.getByteLength());
		configuration.setUnicodeByteLength(listFieldMetaData.getUnicodeByteLength());

		if (listFieldMetaData.getRecordMetaData() != null) {
			RecordMetaDataImpl recordMetaData = configureRecordMetaData(listFieldMetaData.getRecordMetaData());
			configuration.setRecordMetaData(recordMetaData);
		}
		
		return configuration;
    }
    
    private RecordMetaDataImpl configureRecordMetaData(RecordMetaData recordMetaData) {
    		RecordMetaDataImpl configuration = new RecordMetaDataImpl();
    		
    		configuration.setName(recordMetaData.getName());
    		
		if (recordMetaData.getRecordFieldMetaData() != null) {
			for (FieldMetaData fieldMetaData : recordMetaData.getRecordFieldMetaData()) {
				FieldMetaDataImpl recordFieldMetaData = configureFieldMetaData(fieldMetaData);
				configuration.getFieldMetaData().add(recordFieldMetaData);
			}
		}
    		
    		return configuration;
    }
    
    private FieldMetaDataImpl configureFieldMetaData(FieldMetaData fieldMetaData) {
    		FieldMetaDataImpl configuration = new FieldMetaDataImpl();
    		
    		configuration.setName(fieldMetaData.getName());
    		configuration.setDescription(fieldMetaData.getDescription());
    		configuration.setType(DataType.getByName(fieldMetaData.getType()));
    		configuration.setDecimals(fieldMetaData.getDecimals());
    		configuration.setByteLength(fieldMetaData.getByteLength());
    		configuration.setByteOffset(fieldMetaData.getByteOffset());
    		configuration.setUnicodeByteLength(fieldMetaData.getUnicodeByteLength());
    		configuration.setUnicodeByteOffset(fieldMetaData.getUnicodeByteOffset());
    		
    		if (fieldMetaData.getRecordMetaData() != null) {
    			RecordMetaDataImpl recordMetaData = configureRecordMetaData(fieldMetaData.getRecordMetaData());
    			configuration.setRecordMetaData(recordMetaData);
    		}

    		return configuration;
    }
	
}
