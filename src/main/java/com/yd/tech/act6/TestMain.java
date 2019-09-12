package com.yd.tech.act6;

import javax.servlet.http.HttpServletResponse;

public class TestMain {
    public static void main(String[] args) {
        HttpServletResponse response = null;
//        Workflow.start("familyDecorates");
//        Workflow.start("familyDeco");
//        Workflow.commit("240001");
//        Workflow.historicalProcessRecord("240001");
        Workflow.queryNextListActByCurrentAct("250001");
    }
}
