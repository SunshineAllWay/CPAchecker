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
package org.sosy_lab.cpachecker.exceptions;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;

/**
 * Exception thrown if a CPA cannot handle a specific CFAEdge.
 */
public class UnrecognizedCFAEdgeException extends CPATransferException {

    public UnrecognizedCFAEdgeException(CFAEdge edge) {
        super(createMessage(edge));
    }

    private static String createMessage(CFAEdge edge) {
      if (edge.getEdgeType() == CFAEdgeType.MultiEdge) {
        return "Some CPAs do not support MultiEdges. Please set the configuration option \"cfa.useMultiEdges\" to \"false\".";
      }
      return "Unknown CFA edge: " + edge.getEdgeType() + " (" + edge.getDescription() + ")";
    }

    /**
     * auto-generated UID
     */
    private static final long serialVersionUID = -5106215499745787051L;
}
