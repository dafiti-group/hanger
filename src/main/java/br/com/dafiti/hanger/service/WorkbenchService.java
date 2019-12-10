/*
 * Copyright (c) 2019 Dafiti Group
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

import br.com.dafiti.hanger.model.Connection;
import br.com.dafiti.hanger.option.Database;
import br.com.dafiti.hanger.service.ConnectionService.Entity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Fernando Saga
 */
@Service
public class WorkbenchService {

    private final ConnectionService connectionService;

    @Autowired
    public WorkbenchService(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * Identify if should load schema or table list.
     *
     * @param connection Connection
     * @param catalog
     * @param schema
     *
     *
     * @return List Tree
     */
    public List<Tree> JSTreeExchange(
            Connection connection,
            String catalog,
            String schema) {

        if (catalog.isEmpty() && schema.isEmpty()) {
            return JSTreeSchemaList(connection);
        }

        return JSTreeTableList(connection, catalog, schema);
    }

    /**
     * Fully qualified schema list.
     *
     * @param connection Connection
     * @return List schemas tree
     */
    public List<Tree> JSTreeSchemaList(Connection connection) {
        List<Tree> tree = new ArrayList();

        for (Entity schema : connectionService.getSchemas(connection)) {
            tree.add(
                    new Tree(
                            schema.getCatalogSchema(),
                            schema.getCatalogSchema(),
                            "#",
                            "glyphicon glyphicon-th-list",
                            true,
                            new TreeAttribute(
                                    schema.getCatalog(),
                                    schema.getSchema()
                            )
                    ));
        }

        return tree;
    }

    /**
     * Table name list.
     *
     * @param connection Connection
     * @param catalog Catalog
     * @param schema Schema
     * @return Table list
     */
    public List<Tree> JSTreeTableList(
            Connection connection,
            String catalog,
            String schema) {

        List tree = new ArrayList();
        List<Entity> tables = connectionService.getTables(connection, catalog, schema);

        for (Entity table : tables) {
            tree.add(
                    new Tree(
                            table.getTable(),
                            table.getTable(),
                            table.getCatalogSchema(),
                            "glyphicon glyphicon-th-large",
                            false,
                            new TreeAttribute(
                                    table.getCatalog(),
                                    table.getSchema(),
                                    table.getTable(),
                                    connection.getTarget()
                            )
                    )
            );
        }

        return tree;
    }

    /**
     * Represents a JSTree.
     */
    public class Tree {

        String id;
        String text;
        String parent;
        String icon;
        boolean children;
        TreeAttribute a_attr;

        public Tree(
                String id,
                String text,
                String parent,
                String icon,
                boolean children,
                TreeAttribute a_attr) {

            this.id = id;
            this.text = text;
            this.parent = parent;
            this.icon = icon;
            this.children = children;
            this.a_attr = a_attr;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public boolean isChildren() {
            return children;
        }

        public void setChildren(boolean children) {
            this.children = children;
        }

        public TreeAttribute getA_attr() {
            return a_attr;
        }

        public void setA_attr(TreeAttribute a_attr) {
            this.a_attr = a_attr;
        }
    }

    /**
     * Represents a JSTree node attributes.
     */
    public class TreeAttribute {

        String catalog;
        String schema;
        String table;
        Database target;

        public TreeAttribute(String catalog, String schema) {
            this.catalog = catalog;
            this.schema = schema;
        }

        public TreeAttribute(String catalog, String schema, String table, Database target) {
            this.catalog = catalog;
            this.schema = schema;
            this.table = table;
            this.target = target;
        }

        public String getCatalog() {
            return catalog;
        }

        public void setCatalog(String catalog) {
            this.catalog = catalog;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public Database getTarget() {
            return target;
        }

        public void setTarget(Database target) {
            this.target = target;
        }
    }
}