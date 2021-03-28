package org.example.simplespring.demo.controller;

import org.example.simplespring.demo.service.IDemoService;
import org.example.simplespring.framework.annotation.ZAutowired;
import org.example.simplespring.framework.annotation.ZController;
import org.example.simplespring.framework.annotation.ZRequestMapping;
import org.example.simplespring.framework.annotation.ZRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @description: controller层
 * @author: zhangxiaohu
 * @createDate: 2021/3/26
 * @version: 1.0
 */
@ZController
@ZRequestMapping("/demo")
public class DemoController {
    @ZAutowired
    private IDemoService demoService;

    @ZRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @ZRequestParam("/data") String data, @ZRequestParam("/id") String id) {
        String result = demoService.get(data, id);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ZRequestMapping("/add")
    public void add(HttpServletRequest req, HttpServletResponse resp,
                    @ZRequestParam("/data") Integer data,@ZRequestParam("/id") Integer id) {
        try {
            resp.getWriter().write(id + ":" + data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
