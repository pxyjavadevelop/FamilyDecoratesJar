package com.yd.tech.act6.utils;

import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 流程节点
 * @author pxy
 */
public class ProcessesNode {

    /**
     * @description: 获取下一步用户任务节点
     * @param procDefId     流程定义ID
     * @param taskDefKey    当前任务KEY
     * @param map           业务数据
     * @return: java.util.List<org.activiti.bpmn.mapper.UserTask>
     */
    public List<UserTask> getNextNode(String procDefId, String taskDefKey, Map<String, Object> map) {
        List<UserTask> userTasks = new ArrayList<>();

        ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = engine.getRepositoryService();
        //获取Process对象
        BpmnModel bpmnModel = repositoryService.getBpmnModel(procDefId);
        //获取Process对象
        Process process = bpmnModel.getProcesses().get(bpmnModel.getProcesses().size()-1);
        //获取所有的FlowElement信息
        Collection<FlowElement> flowElements = process.getFlowElements();
        //获取当前节点信息
        FlowElement flowElement = getFlowElementById(taskDefKey,flowElements);

        getNextNode(flowElements,flowElement,map,userTasks);

        return userTasks;
    }

    /**
     * @description: 查询下一步节点
     * @param flowElements  全流程节点集合
     * @param flowElement   当前节点
     * @param map           业务数据
     * @param nextUser      下一步用户节点
     * @return: void
     */
    private void getNextNode(Collection<FlowElement> flowElements, FlowElement flowElement, Map<String, Object> map, List<UserTask> nextUser){
        //如果是结束节点
        if(flowElement instanceof EndEvent){
            //如果是子任务的结束节点
            if(getSubProcess(flowElements,flowElement) != null){
                flowElement = getSubProcess(flowElements,flowElement);
            }
        }

        //获取Task的出线信息--可以拥有多个
        List<SequenceFlow> outGoingFlows = null;
        if(flowElement instanceof UserTask){
            outGoingFlows = ((UserTask) flowElement).getOutgoingFlows();
        }else if(flowElement instanceof Gateway){
            outGoingFlows = ((Gateway) flowElement).getOutgoingFlows();
        }else if(flowElement instanceof StartEvent){
            outGoingFlows = ((StartEvent) flowElement).getOutgoingFlows();
        }else if(flowElement instanceof SubProcess){
            outGoingFlows = ((SubProcess) flowElement).getOutgoingFlows();
        }

        if(outGoingFlows != null && outGoingFlows.size()>0) {
            //遍历所有的出线--找到可以正确执行的那一条
            for (SequenceFlow sequenceFlow : outGoingFlows) {
                //1.有表达式，且为true
                //2.无表达式
                String expression = sequenceFlow.getConditionExpression();
                if(StringUtils.isBlank(expression) || Boolean.valueOf(String.valueOf(
                        FelSupport.result(map,expression.substring(expression.lastIndexOf("{")+1,expression.lastIndexOf("}")))))){
                    //出线的下一节点
                    String nextFlowElementID = sequenceFlow.getTargetRef();
                    //查询下一节点的信息
                    FlowElement nextFlowElement = getFlowElementById(nextFlowElementID, flowElements);

                    //用户任务
                    if (nextFlowElement instanceof UserTask) {
                        nextUser.add((UserTask) nextFlowElement);
                    }
                    //排他网关
                    else if (nextFlowElement instanceof ExclusiveGateway) {
                        getNextNode(flowElements, nextFlowElement, map, nextUser);
                    }
                    //并行网关
                    else if (nextFlowElement instanceof ParallelGateway) {
                        getNextNode(flowElements, nextFlowElement, map, nextUser);
                    }
                    //接收任务
                    else if (nextFlowElement instanceof ReceiveTask) {
                        getNextNode(flowElements, nextFlowElement, map, nextUser);
                    }
                    //子任务的起点
                    else if(nextFlowElement instanceof StartEvent){
                        getNextNode(flowElements, nextFlowElement, map, nextUser);
                    }
                    //结束节点
                    else if(nextFlowElement instanceof EndEvent){
                        getNextNode(flowElements, nextFlowElement, map, nextUser);
                    }
                }
            }
        }
    }

    /**
     * @description: 查询一个节点的是否子任务中的节点，如果是，返回子任务
     * @param flowElements 全流程的节点集合
     * @param flowElement   当前节点
     * @return: org.activiti.bpmn.mapper.FlowElement
     */
    private FlowElement getSubProcess(Collection<FlowElement> flowElements,FlowElement flowElement){
        for(FlowElement flowElement1 : flowElements){
            if(flowElement1 instanceof SubProcess){
                for(FlowElement flowElement2 : ((SubProcess) flowElement1).getFlowElements()){
                    if(flowElement.equals(flowElement2)){
                        return flowElement1;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @description: 根据ID查询流程节点对象,如果是子任务，则返回子任务的开始节点
     * @param Id            节点ID
     * @param flowElements  流程节点集合
     * @return: org.activiti.bpmn.mapper.FlowElement
     */
    public FlowElement getFlowElementById(String Id, Collection<FlowElement> flowElements){

        for(FlowElement flowElement : flowElements){
            if(flowElement.getId().equals(Id)){
                //如果是子任务，则查询出子任务的开始节点
                if(flowElement instanceof SubProcess){
                    return getStartFlowElement(((SubProcess) flowElement).getFlowElements());
                }
                return flowElement;
            }
            if(flowElement instanceof SubProcess){
                FlowElement flowElement1 = getFlowElementById(Id,((SubProcess) flowElement).getFlowElements());
                if(flowElement1 != null){
                    return flowElement1;
                }
            }
        }
        return null;
    }

    /**
     * @description: 返回流程的开始节点
     * @param flowElements
     * @return: org.activiti.bpmn.mapper.FlowElement
     */
    private FlowElement getStartFlowElement(Collection<FlowElement> flowElements){
        for (FlowElement flowElement :flowElements){
            if(flowElement instanceof StartEvent){
                return flowElement;
            }
        }
        return null;
    }

    /**
     * @description: 提交至流程结束
     * @param flowElements
     * @return
     */
    public FlowElement commitEndFlowElement(Collection<FlowElement> flowElements) {
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof EndEvent) {
                return flowElement;
            }
        }
        return null;
    }
}
