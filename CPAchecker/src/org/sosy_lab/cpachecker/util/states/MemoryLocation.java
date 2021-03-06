/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.states;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
* This class describes a location in the memory.
*/
public class MemoryLocation implements Comparable<MemoryLocation>, Serializable {

  private static final long serialVersionUID = -8910967707373729034L;
  private final String functionName;
  private final String identifier;
  private final Optional<Long> offset;

  /**
   * This function can be used to {@link com.google.common.collect.Iterables#transform transform}
   * a collection of {@link String}s to a collection of {@link MemoryLocation}s, representing the
   * respective memory location of the identifiers.
   */
  public static final Function<String, MemoryLocation> FROM_STRING_TO_MEMORYLOCATION =
      new Function<String, MemoryLocation>() {
          @Override
          public MemoryLocation apply(String variableName) { return MemoryLocation.valueOf(variableName); }
      };

  /**
   * This function can be used to {@link com.google.common.collect.Iterables#transform transform} a
   * collection of {@link MemoryLocation}s
   * to a collection of {@link String}s, representing the respective variable identifiers.
   */
  public static final Function<MemoryLocation, String> FROM_MEMORYLOCATION_TO_STRING =
      new Function<MemoryLocation, String>() {
          @Override
          public String apply(MemoryLocation memoryLocation) { return memoryLocation.getAsSimpleString(); }
      };

  private MemoryLocation(String pFunctionName, String pIdentifier, Optional<Long> pOffset) {
    checkNotNull(pFunctionName);
    checkNotNull(pIdentifier);

    functionName = pFunctionName;
    identifier = pIdentifier;
    offset = pOffset;
  }

  private MemoryLocation(String pIdentifier, Optional<Long> pOffset) {
    checkNotNull(pIdentifier);

    int separatorIndex = pIdentifier.indexOf("::");
    if (separatorIndex >= 0) {
      functionName = pIdentifier.substring(0, separatorIndex);
      identifier = pIdentifier.substring(separatorIndex + 2);
    } else {
      functionName = null;
      identifier = pIdentifier;
    }
    offset = pOffset;
  }

  @Override
  public boolean equals(Object other) {

    if (this == other) {
      return true;
    }

    if (!(other instanceof MemoryLocation)) {
      return false;
    }

    MemoryLocation otherLocation = (MemoryLocation) other;

    return Objects.equals(functionName, otherLocation.functionName)
        && Objects.equals(identifier, otherLocation.identifier)
        && offset.equals(otherLocation.offset);
  }

  @Override
  public int hashCode() {

    int hc = 17;
    int hashMultiplier = 59;

    hc = hc * hashMultiplier + Objects.hashCode(functionName);
    hc = hc * hashMultiplier + identifier.hashCode();
    hc = hc * hashMultiplier + offset.hashCode();

    return hc;
  }

  public static MemoryLocation valueOf(String pFunctionName, String pIdentifier) {
    return new MemoryLocation(pFunctionName, pIdentifier, Optional.<Long>absent());
  }

  public static MemoryLocation valueOf(String pFunctionName, String pIdentifier, long pOffest) {
    return new MemoryLocation(pFunctionName, pIdentifier, Optional.of(pOffest));
  }

  public static MemoryLocation valueOf(String pIdentifier, long pOffest) {
    return new MemoryLocation(pIdentifier, Optional.of(pOffest));
  }

  public static MemoryLocation valueOf(String pVariableName) {

    String[] nameParts    = pVariableName.split("::");
    String[] offsetParts  = pVariableName.split("/");

    boolean isScoped  = nameParts.length == 2;
    boolean hasOffset = offsetParts.length == 2;

    Optional<Long> offset =
        hasOffset ? Optional.of(Long.parseLong(offsetParts[1])) : Optional.<Long>absent();

    if (isScoped) {
      if (hasOffset) {
        nameParts[1] = nameParts[1].replace("/" + offset.get(), "");
      }
      return new MemoryLocation(nameParts[0], nameParts[1], offset);

    } else {
      if (hasOffset) {
        nameParts[0] = nameParts[0].replace("/" + offset.get(), "");
      }
      return new MemoryLocation(nameParts[0].replace("/" + offset, ""), offset);
    }
  }

  public String getAsSimpleString() {
    String variableName = isOnFunctionStack() ? (functionName + "::" + identifier) : (identifier);
    if (!offset.isPresent()) {
      return variableName;
    }
    return variableName + "/" + offset.get();
  }

  public String serialize() {
    return getAsSimpleString();
  }

  public boolean isOnFunctionStack() {
    return functionName != null;
  }

  public boolean isOnFunctionStack(String pFunctionName) {
    return functionName != null && pFunctionName.equals(functionName);
  }

  public String getFunctionName() {
    return checkNotNull(functionName);
  }

  public String getIdentifier() {
    return identifier;
  }

  public boolean isReference() {
    return offset.isPresent();
  }

  /**
   * Gets the offset of a reference. Only valid for references.
   * See {@link MemoryLocation#isReference()}.
   *
   * @return the offset of a reference.
   */
  public long getOffset() {
    return offset.get();
  }

  @Override
  public String toString() {
    return getAsSimpleString();
  }

  public static PersistentMap<MemoryLocation, Long> transform(
      PersistentMap<String, Long> pConstantMap) {

    PersistentMap<MemoryLocation, Long> result = PathCopyingPersistentTreeMap.of();

    for (Map.Entry<String, Long> entry : pConstantMap.entrySet()) {
      result = result.putAndCopy(valueOf(entry.getKey()), checkNotNull(entry.getValue()));
    }

    return result;
  }

  @Override
  public int compareTo(MemoryLocation other) {

    int result = 0;

    if (isOnFunctionStack()) {
      if (other.isOnFunctionStack()) {
        result = functionName.compareTo(other.functionName);
      } else {
        result = 1;
      }
    } else {
      if (other.isOnFunctionStack()) {
        result = -1;
      } else {
        result = 0;
      }
    }

    if (result != 0) {
      return result;
    }

    return ComparisonChain.start()
        .compare(identifier, other.identifier)
        .compare(offset.orNull(), other.offset.orNull(), Ordering.<Long>natural().nullsFirst())
        .result();
  }
}
