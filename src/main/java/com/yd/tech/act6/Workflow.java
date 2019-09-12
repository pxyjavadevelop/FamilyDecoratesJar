package com.yd.tech.act6;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yd.tech.act6.config.BaseResponse;
import com.yd.tech.act6.config.StatusCode;
import com.yd.tech.act6.exception.MyException;
import com.yd.tech.act6.utils.ActUtil;
import com.yd.tech.act6.utils.FastJsonUtil;
import org.activiti.bpmn.model.*;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;


/**
 * 工作流
 * @author pxy
 */
public class Workflow {

    private static Logger logger = LoggerFactory.getLogger(Workflow.class);

    /**
     * 流程部署
     * @param path :部署的流程图路径
     */
    public static BaseResponse<String> deploy(String path) {

        BaseResponse<String> response;

        if (path.isEmpty()) {
            response = new BaseResponse<>(StatusCode.Invalid_Params);
            logger.info("==============================================");
            logger.error("返回json的参数状态 = {}, 参数描述 = {}",response.getCode(),response.getMsg());
            logger.info("==============================================");
        } else {
            ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
            RepositoryService repositoryService = engine.getRepositoryService();
            Deployment deploy = repositoryService.createDeployment().addClasspathResource(path).deploy();

            String jsonDeploy = JSON.toJSONString(deploy);

            response = new BaseResponse<>(StatusCode.Success);
            response.setData(jsonDeploy);
            logger.info("==============================================");
            logger.info("返回json的参数状态 = {}, 参数描述 = {}",response.getCode(),response.getMsg());
            logger.info("返回json的参数数据 = {}",response.getData());
            logger.info("==============================================");

        }
        return response;

    }

    /**
     * 启动流程
     * @param key:流程图id
     */
    public static BaseResponse<String> start(String key) {

        BaseResponse<String> response;

        if (key.isEmpty()) {
            response = new BaseResponse<>(StatusCode.Invalid_Params);
            logger.info("==============================================");
            logger.error("返回json的参数状态 = {}, 参数描述 = {}", response.getCode(), response.getMsg());
            logger.info("==============================================");
        } else {
            ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
            RuntimeService runtimeService = engine.getRuntimeService();
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("pId",processInstance.getId());
            jsonObject.put("pName",processInstance.getName());
            jsonObject.put("pDefId",processInstance.getProcessDefinitionId());
            jsonObject.put("pActId",processInstance.getActivityId());
            jsonObject.put("pVar",processInstance.getProcessVariables());
            jsonObject.put("pDefName",processInstance.getProcessDefinitionName());
            jsonObject.put("pDeployId",processInstance.getDeploymentId());
            jsonObject.put("startTime",processInstance.getStartTime());

            response = new BaseResponse<>(StatusCode.Success);
            response.setData(FastJsonUtil.toJSON(jsonObject));
            logger.info("==============================================");
            logger.info("json数据 = {}",FastJsonUtil.toJSON(response));
            logger.info("==============================================");
        }

        return response;
    }

    /**
     * 通过实例id进行节点提交
     * @param processInstanceId
     */
    public static BaseResponse<String> commitByProcessInstanceId(String processInstanceId) {
        BaseResponse<String> response = null;
        if (processInstanceId.isEmpty()) {
            response = new BaseResponse<>(StatusCode.Invalid_Params);
            logger.info("==============================================");
            logger.error("返回json的参数状态 = {}, 参数描述 = {}", response.getCode(), response.getMsg());
            logger.info("==============================================");
        } else {
            ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
            TaskService taskService = engine.getTaskService();
            Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
            try {
                taskService.complete(task.getId());
                response = new BaseResponse<>(StatusCode.Success);
            } catch (Exception e) {
                e.printStackTrace();
                response = new BaseResponse<>(StatusCode.NotFound);
            }finally {
                logger.info("==============================================");
                logger.info("json数据 = {}",FastJsonUtil.toJSON(response));
                logger.info("==============================================");
            }
        }
        return response;
    }

    /**
     * 通过节点id进行提交
     * @param taskId
     */
    public static BaseResponse<String> commitByTaskId(String taskId) {
        BaseResponse<String> response = null;
        if (taskId.isEmpty()) {
            response = new BaseResponse<>(StatusCode.Invalid_Params);
            logger.info("==============================================");
            logger.error("返回json的参数状态 = {}, 参数描述 = {}", response.getCode(), response.getMsg());
            logger.info("==============================================");
        } else {
            ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
            TaskService taskService = engine.getTaskService();
            try {
                taskService.complete(taskId);
                response = new BaseResponse<>(StatusCode.Success);
            } catch (MyException me) {
                response = new BaseResponse<>(StatusCode.NotFound);
                throw new MyException("==== taskId 有误 ====");
            }finally {
                logger.info("==============================================");
                logger.info("json数据 = {}",FastJsonUtil.toJSON(response));
                logger.info("==============================================");
            }
        }
        return response;
    }

    /**
     * 获取当前节点的下一环节列表和列表中每个环节对应的信息
     * @param processInstanceId
     */
    public static BaseResponse<String> queryNextListActByCurrentAct(String processInstanceId) {
        BaseResponse<String> response = null;
        if (processInstanceId.isEmpty()) {
            response = new BaseResponse<>(StatusCode.Invalid_Params);
            logger.info("==============================================");
            logger.error("返回json的参数状态 = {}, 参数描述 = {}", response.getCode(), response.getMsg());
            logger.info("==============================================");
        } else {
            ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
            TaskService taskService = engine.getTaskService();
            RuntimeService runtimeService = engine.getRuntimeService();
            RepositoryService repositoryService = engine.getRepositoryService();
            try {
                Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
                //当前UserTask的所有信息
                Execution execution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
                //获取流程画线对象
                BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
                FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(execution.getActivityId());
                List<SequenceFlow> outgoingFlows = flowNode.getOutgoingFlows();
                JSONObject jsonObject = new JSONObject();
                Iterator<SequenceFlow> iterator = outgoingFlows.iterator();
                List<String> listActId = new ArrayList<>();
                List<String> listActName = new ArrayList<>();
                List<String> listActAssginee = new ArrayList<>();
                UserTask userTask;
                while (iterator.hasNext()) {
                    String targetRef = iterator.next().getTargetRef();
                    FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement(targetRef);
                    if (flowElement instanceof UserTask) {
                        userTask =(UserTask)flowElement;
                        String actId = userTask.getId();
                        String actName = userTask.getName();
                        String actAssignee = userTask.getAssignee();
                        listActId.add(actId);
                        listActName.add(actName);
                        listActAssginee.add(actAssignee);
                    }

                }
                response = new BaseResponse<>(StatusCode.Success);
                response.setData(String.valueOf(listActId));
                response.setData(String.valueOf(listActName));
                response.setData(String.valueOf(listActAssginee));
            }catch (MyException me) {
                response = new BaseResponse<>(StatusCode.NotFound);
                throw new MyException("==== taskId 有误 ====");
            }finally {
                logger.info("==============================================");
                logger.info("json数据 = {}",FastJsonUtil.toJSON(response));
                logger.info("==============================================");
            }
        }
        return response;
    }

    /**
     * 历史流程记录详情
     * @param processInstanceId
     */
    public static BaseResponse<String> historicalProcessRecord(String processInstanceId) {
        BaseResponse<String> response = null;
        if (processInstanceId.isEmpty()) {
            response = new BaseResponse<>(StatusCode.Invalid_Params);
            logger.info("==============================================");
            logger.error("返回json的参数状态 = {}, 参数描述 = {}", response.getCode(), response.getMsg());
            logger.info("==============================================");
        } else {
            ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
            HistoryService historyService = engine.getHistoryService();
            try {
                response = new BaseResponse<>(StatusCode.Success);
                List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();
                Iterator<HistoricActivityInstance> iterator = list.iterator();
                response.setData(FastJsonUtil.toJSON(iterator));
            } catch (MyException me) {
                response = new BaseResponse<>(StatusCode.NotFound);
                me.printStackTrace();
            }finally {
                logger.info("==============================================");
                logger.info("json数据 = {}",FastJsonUtil.toJSON(response));
                logger.info("==============================================");
            }
        }
        return response;
    }

    /**
     * 流程跟踪图
     * @param processInstanceId
     * @param response
     */
    public static void getFlowImgByInstantId(String processInstanceId, HttpServletResponse response) {
        try {
            ActUtil.getFlowImgByInstanceId(processInstanceId, response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
