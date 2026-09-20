package com.zifang.z.script.web.controller;

import com.zifang.z.script.core.domain.entity.ScriptTagDO;
import com.zifang.z.script.core.domain.service.impl.ScriptTagServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/script/tag")
public class ScriptTagController {

    @Autowired
    private ScriptTagServiceImpl tagService;

    @PostMapping("/create")
    // D24: 前端 ScriptProductizationPage 通过 fetch + JSON body 传 {tagName, tagCategory, tagColor},
    // 旧 @RequestParam 接收不到 JSON body. 改用 @RequestBody 接收 Map.
    public Map<String, Object> create(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null) {
            throw new IllegalArgumentException("request body required");
        }
        String tagName = (String) body.get("tagName");
        String tagCategory = (String) body.get("tagCategory");
        String tagColor = (String) body.get("tagColor");
        ScriptTagDO tag = tagService.createTag(tagName, tagCategory, tagColor);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", tag);
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> list() {
        List<ScriptTagDO> tags = tagService.listAll();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", tags);
        return result;
    }

    @PostMapping("/bind")
    public Map<String, Object> bind(@RequestParam Long scriptId, @RequestParam Long tagId) {
        tagService.addTagToScript(scriptId, tagId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @PostMapping("/unbind")
    public Map<String, Object> unbind(@RequestParam Long scriptId, @RequestParam Long tagId) {
        tagService.removeTagFromScript(scriptId, tagId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @GetMapping("/of-script/{scriptId}")
    public Map<String, Object> ofScript(@PathVariable Long scriptId) {
        List<ScriptTagDO> tags = tagService.listTagsOfScript(scriptId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", tags);
        return result;
    }

    @DeleteMapping("/{tagId}")
    public Map<String, Object> delete(@PathVariable Long tagId) {
        boolean ok = tagService.deleteTag(tagId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        return result;
    }
}