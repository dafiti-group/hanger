/*
 * Copyright (c) 2020 Dafiti Group
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package br.com.dafiti.hanger.service;

import br.com.dafiti.hanger.model.Template;
import br.com.dafiti.hanger.repository.TemplateRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

/**
 *
 * @author Valdiney V GOMES
 */
@Service
public class TemplateService {

    private final TemplateRepository templateRepository;

    @Autowired
    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Cacheable(value = "templates")
    public Iterable<Template> list() {
        return templateRepository.findAll();
    }

    public Template load(Long id) {
        return templateRepository.findById(id).get();
    }

    @Caching(evict = {
        @CacheEvict(value = "templates", allEntries = true)})
    public void save(Template template) {
        templateRepository.save(template);
    }

    @Caching(evict = {
        @CacheEvict(value = "templates", allEntries = true)})
    public void delete(Long id) {
        templateRepository.deleteById(id);
    }

    /**
     * Extract template parameters.
     *
     * @param template Template.
     * @return Template parameters and type list.
     */
    public Map<String, Map<String, String>> getParameters(String template) {
        Map<String, Map<String, String>> parameter = new HashMap();
        Matcher m = Pattern.compile("\\$\\{\\{([^\\s]+)\\}\\}").matcher(template);

        while (m.find()) {
            String name = m.group(1);

            if (!parameter.containsKey(name)) {
                Map<String, String> attributes = new HashMap();
                String[] split = name.split("::");

                attributes.put("name", split[0]);

                if (split.length == 2) {
                    String type;
                    String defaultValue;

                    try {
                        JSONObject object = new JSONObject(split[1]);
                        type = object.optString("type", "text");
                        defaultValue = String.valueOf(object.opt("default"));
                    } catch (JSONException ex) {
                        type = "text";
                        defaultValue = "";
                    }

                    attributes.put("type", type);
                    attributes.put("default", defaultValue);

                    parameter.put(name, attributes);
                } else {
                    attributes.put("type", "text");
                    attributes.put("default", "");

                    parameter.put(name, attributes);
                }
            }
        }

        return parameter;
    }

    /**
     * Replace dynamic template parameters.
     *
     * @param template Template
     * @param parameters Template parameters
     * @return Template final
     */
    public String setParameters(String template, JSONArray parameters) {
        if (parameters != null
                && !parameters.isEmpty()) {

            for (Object parameter : parameters) {
                JSONObject object = (JSONObject) parameter;
                template = template.replaceAll(
                        "\\$\\{\\{"
                                .concat(object.getString("name")
                                        .replace("{", "\\{")
                                        .replace("}", "\\}")).concat("\\}\\}"),
                        object.getString("value")
                );
            }
        }

        return template;
    }
}
