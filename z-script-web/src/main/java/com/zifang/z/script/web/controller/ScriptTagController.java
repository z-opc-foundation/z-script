package com.zifang.z.script.web.controller;

import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
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
    @BizApi(summary = "create", description = "", tags = { "z-script" })
    // D24: 前端 ScriptProductizationPage 通过 fetch + JSON body 传 {tagName, tagCategory, tagColor},
    // 旧 @RequestParam 接收不到 JSON body. 改用 @RequestBody 接收 Map.
    public Map<String, Object> create(@RequestBody(required = false) @BizParam(name = "body", type = BizField.FieldType.JSON, description = "", position = BizParam.Position.BODY) Map<String, Object> body) {
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
    @BizApi(summary = "list", description = "", tags = { "z-script" })
    public Map<String, Object> list() {
        List<ScriptTagDO> tags = tagService.listAll();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", tags);
        return result;
    }

    @PostMapping("/bind")
    @BizApi(summary = "bind", description = "", tags = { "z-script" })
    public Map<String, Object> bind(@RequestParam @BizParam(name = "scriptId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long scriptId, @RequestParam @BizParam(name = "tagId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long tagId) {
        tagService.addTagToScript(scriptId, tagId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @PostMapping("/unbind")
    @BizApi(summary = "unbind", description = "", tags = { "z-script" })
    public Map<String, Object> unbind(@RequestParam @BizParam(name = "scriptId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long scriptId, @RequestParam @BizParam(name = "tagId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long tagId) {
        tagService.removeTagFromScript(scriptId, tagId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @GetMapping("/of-script/{scriptId}")
    @BizApi(summary = "ofScript", description = "", tags = { "z-script" })
    public Map<String, Object> ofScript(@PathVariable @BizParam(name = "scriptId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.PATH) Long scriptId) {
        List<ScriptTagDO> tags = tagService.listTagsOfScript(scriptId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", tags);
        return result;
    }

    @DeleteMapping("/{tagId}")
    @BizApi(summary = "delete", description = "", tags = { "z-script" })
    public Map<String, Object> delete(@PathVariable @BizParam(name = "tagId", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.PATH) Long tagId) {
        boolean ok = tagService.deleteTag(tagId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        return result;
    }
}

