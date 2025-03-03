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

package io.glutenproject.execution

import java.io.Serializable

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import io.glutenproject.backendsapi.BackendsApiManager
import io.glutenproject.GlutenConfig
import io.glutenproject.vectorized._
import org.apache.spark._

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.expressions.Attribute
import org.apache.spark.sql.connector.read.InputPartition
import org.apache.spark.sql.execution.datasources.PartitionedFile
import org.apache.spark.sql.vectorized.ColumnarBatch
import org.apache.spark.util._

trait BaseNativeFilePartition extends Partition with InputPartition {
  def substraitPlan: Array[Byte]
}

case class NativeFilePartition(index: Int, files: Array[PartitionedFile],
                               substraitPlan: Array[Byte])
  extends BaseNativeFilePartition {
  override def preferredLocations(): Array[String] = {
    // Computes total number of bytes can be retrieved from each host.
    val hostToNumBytes = mutable.HashMap.empty[String, Long]
    files.foreach { file =>
      file.locations.filter(_ != "localhost").foreach { host =>
        hostToNumBytes(host) = hostToNumBytes.getOrElse(host, 0L) + file.length
      }
    }

    // Takes the first 3 hosts with the most data to be retrieved
    hostToNumBytes.toSeq.sortBy {
      case (host, numBytes) => numBytes
    }.reverse.take(3).map {
      case (host, numBytes) => host
    }.toArray
  }
}

case class NativeSubstraitPartition(val index: Int, val inputPartition: InputPartition)
  extends Partition with Serializable

class NativeWholeStageColumnarRDD(
    sc: SparkContext,
    @transient private val inputPartitions: Seq[InputPartition],
    columnarReads: Boolean,
    outputAttributes: Seq[Attribute],
    jarList: Seq[String],
    dependentKernelIterators: ListBuffer[GeneralOutIterator])
    extends RDD[ColumnarBatch](sc, Nil) {
  val numaBindingInfo = GlutenConfig.getConf.numaBindingInfo
  val loadNative: Boolean = GlutenConfig.getConf.loadNative

  override protected def getPartitions: Array[Partition] = {
    inputPartitions.zipWithIndex.map {
      case (inputPartition, index) => new NativeSubstraitPartition(index, inputPartition)
    }.toArray
  }

  private def castPartition(split: Partition): NativeSubstraitPartition = split match {
    case p: NativeSubstraitPartition => p
    case _ => throw new SparkException(s"[BUG] Not a NativeSubstraitPartition: $split")
  }

  private def castNativePartition(split: Partition): BaseNativeFilePartition = split match {
    case NativeSubstraitPartition(_, p: BaseNativeFilePartition) => p
    case _ => throw new SparkException(s"[BUG] Not a NativeSubstraitPartition: $split")
  }

  override def compute(split: Partition, context: TaskContext): Iterator[ColumnarBatch] = {
    ExecutorManager.tryTaskSet(numaBindingInfo)

    val inputPartition = castNativePartition(split)

    BackendsApiManager.getIteratorApiInstance.genFirstStageIterator(inputPartition, loadNative,
      outputAttributes, context, jarList)
  }

  override def getPreferredLocations(split: Partition): Seq[String] = {
    castPartition(split).inputPartition.preferredLocations()
  }

}
