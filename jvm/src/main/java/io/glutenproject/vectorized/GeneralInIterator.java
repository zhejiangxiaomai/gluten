/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.glutenproject.vectorized;

import org.apache.spark.sql.vectorized.ColumnarBatch;

import java.util.Iterator;

abstract public class GeneralInIterator implements AutoCloseable {
  protected final Iterator<ColumnarBatch> delegated;
  private transient ColumnarBatch nextBatch = null;

  public GeneralInIterator(Iterator<ColumnarBatch> delegated) {
    this.delegated = delegated;
  }

  public boolean hasNext() {
    while (delegated.hasNext()) {
      nextBatch = delegated.next();
      if (nextBatch.numRows() > 0) {
        // any problem using delegated.hasNext() instead?
        return true;
      }
    }
    return false;
  }

  @Override
  public void close() throws Exception {
  }

  public ColumnarBatch nextColumnarBatch() {
    return nextBatch;
  }
}
