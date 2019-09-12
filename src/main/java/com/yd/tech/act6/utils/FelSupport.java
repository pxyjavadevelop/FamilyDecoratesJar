package com.yd.tech.act6.utils;

import com.greenpineyu.fel.FelEngine;
import com.greenpineyu.fel.FelEngineImpl;
import com.greenpineyu.fel.context.FelContext;

import java.util.Map;

/**
 * 轻量级高效表达式计算引擎
 */
public class FelSupport {
    public static Object result(Map<String,Object> map, String expression){
        FelEngine fel = new FelEngineImpl();
        FelContext ctx = fel.getContext();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            ctx.set(entry.getKey(),entry.getValue());
        }
        Object result = fel.eval(expression);
        return result;
    }
}
