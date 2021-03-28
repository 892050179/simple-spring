package org.example.simplespring.demo.service.impl;

import org.example.simplespring.demo.service.IDemoService;
import org.example.simplespring.framework.annotation.ZService;

/**
 * @description: 服务实现类
 * @author: zhangxiaohu
 * @createDate: 2021/3/26
 * @version: 1.0
 */
@ZService
public class DemoService implements IDemoService {

    @Override
    public String get(String id, String data) {
        return "This is " + data + ",id = " + id;
    }
}
