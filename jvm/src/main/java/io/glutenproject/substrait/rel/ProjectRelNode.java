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

package io.glutenproject.substrait.rel;

import io.glutenproject.substrait.expression.ExpressionNode;
import io.glutenproject.substrait.extensions.AdvancedExtensionNode;
import io.substrait.proto.ProjectRel;
import io.substrait.proto.Rel;
import io.substrait.proto.RelCommon;

import java.io.Serializable;
import java.util.ArrayList;

public class ProjectRelNode implements RelNode, Serializable {
    private final RelNode input;
    private final ArrayList<ExpressionNode> expressionNodes =
            new ArrayList<>();
    private final AdvancedExtensionNode extensionNode;

    ProjectRelNode(RelNode input,
                   ArrayList<ExpressionNode> expressionNodes) {
        this.input = input;
        this.expressionNodes.addAll(expressionNodes);
        this.extensionNode = null;
    }

    ProjectRelNode(RelNode input,
                   ArrayList<ExpressionNode> expressionNodes,
                   AdvancedExtensionNode extensionNode) {
        this.input = input;
        this.expressionNodes.addAll(expressionNodes);
        this.extensionNode = extensionNode;
    }

    @Override
    public Rel toProtobuf() {
        RelCommon.Builder relCommonBuilder = RelCommon.newBuilder();
        relCommonBuilder.setDirect(RelCommon.Direct.newBuilder());

        ProjectRel.Builder projectBuilder = ProjectRel.newBuilder();
        projectBuilder.setCommon(relCommonBuilder.build());
        if (input != null) {
            projectBuilder.setInput(input.toProtobuf());
        }
        for (ExpressionNode expressionNode : expressionNodes) {
            projectBuilder.addExpressions(expressionNode.toProtobuf());
        }
        if (extensionNode != null) {
            projectBuilder.setAdvancedExtension(extensionNode.toProtobuf());
        }
        Rel.Builder builder = Rel.newBuilder();
        builder.setProject(projectBuilder.build());
        return builder.build();
    }
}
