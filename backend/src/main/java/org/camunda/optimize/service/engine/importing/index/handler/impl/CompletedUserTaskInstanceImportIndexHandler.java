package org.camunda.optimize.service.engine.importing.index.handler.impl;

import org.camunda.optimize.rest.engine.EngineContext;
import org.camunda.optimize.service.engine.importing.index.handler.TimestampBasedImportIndexHandler;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CompletedUserTaskInstanceImportIndexHandler extends TimestampBasedImportIndexHandler {

  private static final String COMPLETED_USER_TASK_INSTANCE_IMPORT_INDEX_DOC_ID = "completedUserTaskInstanceImportIndex";

  public CompletedUserTaskInstanceImportIndexHandler(EngineContext engineContext) {
    super(engineContext);
  }

  @Override
  protected String getElasticsearchDocID() {
    return COMPLETED_USER_TASK_INSTANCE_IMPORT_INDEX_DOC_ID;
  }

}
