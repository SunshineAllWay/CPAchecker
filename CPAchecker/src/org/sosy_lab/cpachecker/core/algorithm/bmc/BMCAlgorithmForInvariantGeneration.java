/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.Objects;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.base.Verify;

public class BMCAlgorithmForInvariantGeneration extends AbstractBMCAlgorithm {

  private final CandidateGenerator candidateGenerator;

  private InvariantSupplier locationInvariantsProvider = InvariantSupplier.TrivialInvariantSupplier.INSTANCE;

  private ExpressionTreeSupplier locationInvariantExpressionTreeProvider = ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;

  public BMCAlgorithmForInvariantGeneration(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA,
                      Configuration pConfig, LogManager pLogger,
                      ReachedSetFactory pReachedSetFactory,
                      ShutdownManager pShutdownManager, CFA pCFA,
                      BMCStatistics pBMCStatistics,
                      CandidateGenerator pCandidateGenerator)
                      throws InvalidConfigurationException, CPAException {
    super(pAlgorithm, pCPA, pConfig, pLogger, pReachedSetFactory, pShutdownManager, pCFA,
        pBMCStatistics,
        true /* invariant generator */ );
    Verify.verify(checkIfInductionIsPossible(pCFA, pLogger));
    candidateGenerator = Objects.requireNonNull(pCandidateGenerator);
  }

  public InvariantSupplier getCurrentInvariants() {
    return locationInvariantsProvider;
  }

  public ExpressionTreeSupplier getCurrentInvariantsAsExpressionTree() {
    return locationInvariantExpressionTreeProvider;
  }

  public boolean isProgramSafe() {
    return invariantGenerator.isProgramSafe();
  }

  @Override
  protected CandidateGenerator getCandidateInvariants() {
    return candidateGenerator;
  }

  @Override
  protected KInductionProver createInductionProver() {
    final KInductionProver prover = super.createInductionProver();

    if (prover != null) {
      locationInvariantsProvider = new InvariantSupplier() {

        @Override
        public BooleanFormula getInvariantFor(CFANode location, FormulaManagerView fmgr, PathFormulaManager pfmgr) {
          try {
            return prover.getCurrentLocationInvariants(location, fmgr, pfmgr);
          } catch (InterruptedException | CPAException e) {
            return fmgr.getBooleanFormulaManager().makeBoolean(true);
          }
        }
      };
      locationInvariantExpressionTreeProvider =
          new ExpressionTreeSupplier() {

            @Override
            public ExpressionTree<Object> getInvariantFor(CFANode location) {
              try {
                return prover.getCurrentLocationInvariants(location);
              } catch (InterruptedException e) {
                return ExpressionTrees.getTrue();
              }
            }
          };
    }

    return prover;
  }
}