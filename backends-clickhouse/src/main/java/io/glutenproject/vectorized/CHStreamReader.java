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

import java.io.InputStream;

public class CHStreamReader implements AutoCloseable {
  private long nativeShuffleReader;
  private final InputStream inputStream;

  private static native long createNativeShuffleReader(InputStream inputStream);

  public CHStreamReader(InputStream inputStream) {
    this.inputStream = inputStream;
    nativeShuffleReader = createNativeShuffleReader(this.inputStream);
  }

  private native long nativeNext(long nativeShuffleReader);

  public CHNativeBlock next() {
    long block = nativeNext(nativeShuffleReader);
    return new CHNativeBlock(block);
  }

  private native void nativeClose(long shuffleReader);

  @Override
  public void close() throws Exception {
    nativeClose(nativeShuffleReader);
    nativeShuffleReader = 0L;
  }

}
