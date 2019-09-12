package com.yd.tech.act6.utils;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.image.ProcessDiagramGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 流程工具
 *
 * @author pxy
 */
public class ActUtil {

    private static Logger logger = LoggerFactory.getLogger(ActUtil.class);
    /**
     * 根据流程实例Id,获取实时流程图片
     * @param processInstanceId
     * @param outputStream
     */
    public static void getFlowImgByInstanceId(String processInstanceId, OutputStream outputStream) {

        ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = engine.getRepositoryService();
        HistoryService historyService = engine.getHistoryService();
        ProcessEngineConfiguration processEngineConfiguration = engine.getProcessEngineConfiguration();

        try {
            if (processInstanceId.isEmpty()) {
                logger.error("processInstanceId is null");
                return;
            }
            // 通过流程实例id获取历史流程实例
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            // 获取流程中已经执行的节点，按照执行先后顺序排序
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId)
                    .orderByHistoricActivityInstanceId().asc().list();
            // 高亮已经执行流程节点ID集合
            List<String> highLightedActivitiIds = new ArrayList<>();
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                highLightedActivitiIds.add(historicActivityInstance.getActivityId());
            }
            // 查询完成的历史流程实例（已经提交至end节点）
            List<HistoricProcessInstance> historicFinishedProcessInstances = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).finished().list();
            ProcessDiagramGenerator processDiagramGenerator = null;
            // 如果还没完成，流程图高亮颜色为红色，如果已经完成为绿色
            if (!CollectionUtils.isEmpty(historicFinishedProcessInstances)) {
                processDiagramGenerator = new CustomProcessDiagramGenerator();
                // 如果不为空，说明未完成
            } else {
                processDiagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            }
            // 获取流程图模型
            BpmnModel bpmnModel = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId());
            // 高亮流程已发生流转的线id集合
            List<String> highLightedFlowIds = getHighLightedFlowsByProcessInstanceId(processInstanceId);
            // 使用默认配置获得流程图表生成器，并生成追踪图片字符流
            InputStream imageStream = processDiagramGenerator.generateDiagram(bpmnModel, "png", highLightedActivitiIds, highLightedFlowIds, "宋体", "微软雅黑", "黑体", null, 2.0);
            // 输出图片内容
            byte[] b = new byte[1024];
            int len;
            while ((len = imageStream.read(b, 0, 1024)) != -1) {
                outputStream.write(b, 0, len);
            }
        } catch (Exception e) {
            logger.error("processInstanceId" + processInstanceId + "生成流程图失败，原因：" + e.getMessage(), e);
        }
    }

    /**
     * 通过历史活动节点排序进行画线
     * @param processInstanceId
     * @return 高亮线的id集合
     */
    private static List<String> getHighLightedFlowsByProcessInstanceId(String processInstanceId) {

        ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
        HistoryService historyService = engine.getHistoryService();
        RepositoryService repositoryService = engine.getRepositoryService();

        //排序
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();
        //通过bpmn流程图id
        String processDefinitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("familyDecorates").latestVersion().singleResult().getId();
        Process process = repositoryService.getBpmnModel(processDefinitionId).getProcesses().get(0);
        Collection<FlowElement> flowElements = process.getFlowElements();
        //创建高亮线id集合
        List<String> highLightedFlowIds = new ArrayList<>();
        //通过流程实例id获取历史流程活动节点id,再通过流程画线判断；如果源节点id和目标节点id等于历史流程活动节点顺序id,则获取流程线id
        for (int i=0;i<list.size()-1;i++){
            for (FlowElement flowElement : flowElements) {
                if (flowElement instanceof SequenceFlow) {
                    String sourceRef = ((SequenceFlow) flowElement).getSourceRef();
                    String targetRef = ((SequenceFlow) flowElement).getTargetRef();
                    if (list.get(i).getActivityId().equals(sourceRef) && list.get(i + 1).getActivityId().equals(targetRef)) {
                        highLightedFlowIds.add(flowElement.getId());
                    }
                }
            }
        }
        return highLightedFlowIds;
    }
}
