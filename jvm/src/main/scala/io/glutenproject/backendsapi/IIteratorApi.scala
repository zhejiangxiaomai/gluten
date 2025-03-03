/*
 * Copyright (2021) The Delta Lake Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.glutenproject.backendsapi

import io.glutenproject.GlutenNumaBindingInfo
import io.glutenproject.execution.{BaseNativeFilePartition, WholestageTransformContext}
import io.glutenproject.substrait.plan.PlanNode
import io.glutenproject.vectorized.{ExpressionEvaluator, ExpressionEvaluatorJniWrapper, GeneralInIterator, GeneralOutIterator}
import org.apache.spark.{SparkConf, TaskContext}
import org.apache.spark.sql.catalyst.expressions.Attribute
import org.apache.spark.sql.connector.read.InputPartition
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.execution.metric.SQLMetric
import org.apache.spark.sql.vectorized.ColumnarBatch

trait IIteratorApi extends IBackendsApi {

  /**
   * Generate native row partition.
   *
   * @return
   */
  def genNativeFilePartition(p: InputPartition,
                             wsCxt: WholestageTransformContext): BaseNativeFilePartition

  /**
   * Generate Iterator[ColumnarBatch] for CoalesceBatchesExec.
   *
   * @return
   */
  def genCoalesceIterator(iter: Iterator[ColumnarBatch],
                          recordsPerBatch: Int, numOutputRows: SQLMetric,
                          numInputBatches: SQLMetric, numOutputBatches: SQLMetric,
                          collectTime: SQLMetric, concatTime: SQLMetric,
                          avgCoalescedNumRows: SQLMetric): Iterator[ColumnarBatch]

  /**
   * Generate closeable ColumnBatch iterator.
   *
   * @return
   */
  def genCloseableColumnBatchIterator(iter: Iterator[ColumnarBatch]): Iterator[ColumnarBatch]

  /**
   * Generate Iterator[ColumnarBatch] for first stage.
   *
   * @return
   */
  def genFirstStageIterator(inputPartition: BaseNativeFilePartition, loadNative: Boolean,
                            outputAttributes: Seq[Attribute], context: TaskContext,
                            jarList: Seq[String]): Iterator[ColumnarBatch]

  /**
   * Generate Iterator[ColumnarBatch] for final stage.
   *
   * @return
   */
  def genFinalStageIterator(inputIterators: Seq[Iterator[ColumnarBatch]],
                            numaBindingInfo: GlutenNumaBindingInfo, listJars: Seq[String],
                            signature: String, sparkConf: SparkConf,
                            outputAttributes: Seq[Attribute], rootNode: PlanNode,
                            streamedSortPlan: SparkPlan, pipelineTime: SQLMetric,
                            buildRelationBatchHolder: Seq[ColumnarBatch],
                            dependentKernels: Seq[ExpressionEvaluator],
                            dependentKernelIterators: Seq[GeneralOutIterator]
                           ): Iterator[ColumnarBatch]

  /**
   * Generate columnar native iterator.
   *
   * @return
   */
  def genColumnarNativeIterator(delegated: Iterator[ColumnarBatch]): GeneralInIterator

  /**
   * Generate BatchIterator for ExpressionEvaluator.
   *
   * @return
   */
  def genBatchIterator(wsPlan: Array[Byte], iterList: Seq[GeneralInIterator],
                       jniWrapper: ExpressionEvaluatorJniWrapper): GeneralOutIterator
}
