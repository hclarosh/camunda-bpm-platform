/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.impl.migration.instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.camunda.bpm.engine.impl.bpmn.parser.EventSubscriptionDeclaration;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.migration.MigrationInstruction;
import org.camunda.bpm.engine.runtime.ActivityInstance;

/**
 * @author Thorben Lindhauer
 *
 */
public abstract class MigratingActivityInstance implements MigratingInstance, RemovingInstance {

  protected MigrationInstruction migrationInstruction;
  protected ActivityInstance activityInstance;
  // scope execution for actual scopes,
  // concurrent execution in case of non-scope activity with expanded tree
  protected ExecutionEntity representativeExecution;

  protected List<RemovingInstance> removingDependentInstances = new ArrayList<RemovingInstance>();
  protected List<MigratingInstance> migratingDependentInstances = new ArrayList<MigratingInstance>();
  protected List<EmergingInstance> emergingDependentInstances = new ArrayList<EmergingInstance>();

  protected ScopeImpl sourceScope;
  protected ScopeImpl targetScope;

  protected Set<MigratingActivityInstance> childInstances;
  protected MigratingActivityInstance parentInstance;

  public abstract void detachState();

  public abstract void attachState(ExecutionEntity newScopeExecution);

  protected void removeTimerJobs(ExecutionEntity currentExecution) {
    Context.getCommandContext()
      .getJobManager()
      .cancelTimers(currentExecution);
  }

  public void migrateDependentEntities() {
    for (MigratingInstance migratingInstance : migratingDependentInstances) {
      migratingInstance.migrateState();
      migratingInstance.migrateDependentEntities();
    }

    ExecutionEntity representativeExecution = resolveRepresentativeExecution();
    for (EmergingInstance emergingInstance : emergingDependentInstances) {
      emergingInstance.create(representativeExecution);
    }
  }

  public abstract ExecutionEntity resolveRepresentativeExecution();

  public void addMigratingDependentInstance(MigratingInstance migratingInstance) {
    migratingDependentInstances.add(migratingInstance);
  }

  public void addRemovingDependentInstance(RemovingInstance removingInstance) {
    removingDependentInstances.add(removingInstance);
  }

  public void addEmergingDependentInstance(EmergingInstance emergingInstance) {
    emergingDependentInstances.add(emergingInstance);
  }

  protected void createMissingTimerJobs(ExecutionEntity currentScopeExecution) {
    currentScopeExecution.initializeTimerDeclarations();
  }

  public ActivityInstance getActivityInstance() {
    return activityInstance;
  }

  public ScopeImpl getSourceScope() {
    return sourceScope;
  }

  public ScopeImpl getTargetScope() {
    return targetScope;
  }

  public Set<MigratingActivityInstance> getChildren() {
    return childInstances;
  }

  public MigratingActivityInstance getParent() {
    return parentInstance;
  }

  public void setParent(MigratingActivityInstance parentInstance) {
    this.parentInstance = parentInstance;
  }

  public MigrationInstruction getMigrationInstruction() {
    return migrationInstruction;
  }

  public boolean migrates() {
    return targetScope != null;
  }

  public void removeUnmappedDependentInstances() {
    for (RemovingInstance removingInstance : removingDependentInstances) {
      removingInstance.remove();
    }
  }

}


