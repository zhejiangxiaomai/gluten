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

package org.apache.spark.sql.execution.datasources.v2

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.QueryPlan
import org.apache.spark.sql.connector.read.{InputPartition, PartitionReaderFactory, Scan}

// This file is copied from Spark with a little change to solve below issue:
// The BatchScanExec can support columnar output, which is incompatible with
// Arrow's columnar format. But there is no config to disable the columnar output.
// In this file, the supportsColumnar was set as false to prevent Spark's columnar
// output.
/**
 * Physical plan node for scanning a batch of data from a data source v2.
 */
case class BatchScanExec(output: Seq[AttributeReference],
                         @transient scan: Scan) extends DataSourceV2ScanExecBase {

  @transient lazy val batch = scan.toBatch

  // TODO: unify the equal/hashCode implementation for all data source v2 query plans.
  override def equals(other: Any): Boolean = other match {
    case other: BatchScanExec => this.batch == other.batch
    case _ => false
  }

  override def hashCode(): Int = batch.hashCode()

  // Set to false to disable BatchScan's columnar output.
  override def supportsColumnar: Boolean = false

  @transient override lazy val partitions: Seq[InputPartition] = batch.planInputPartitions()

  override lazy val readerFactory: PartitionReaderFactory = batch.createReaderFactory()

  override lazy val inputRDD: RDD[InternalRow] = {
    new DataSourceRDD(sparkContext, partitions, readerFactory, supportsColumnar)
  }

  override def doCanonicalize(): BatchScanExec = {
    this.copy(output = output.map(QueryPlan.normalizeExpressions(_, output)))
  }
}
