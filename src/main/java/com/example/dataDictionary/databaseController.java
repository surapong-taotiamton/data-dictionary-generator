package com.example.dataDictionary;
import io.r2dbc.spi.ColumnMetadata;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class databaseController {

    @PostMapping("/upload")
    public String handleFormSubmission(@RequestParam("username") String username,
                                       @RequestParam("password") String password,
                                       @RequestParam("server") String server,
                                       @RequestParam("port") String port,
                                       @RequestParam("database") String database,
                                       Model model) {

        String dbUrl = "jdbc:mysql://" + server + ":" + port + "/" + database;

        try (Connection connection = DriverManager.getConnection(dbUrl, username, password)) {
            String selectQuery = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
                preparedStatement.setString(1, database);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    // Fetch table names and comments from the database
                    List<String[]> tableInfoList = new ArrayList<>();
                    while (resultSet.next()) {
                        String tableName = resultSet.getString("TABLE_NAME");
                        String tableComment = resultSet.getString("TABLE_COMMENT");

                        tableInfoList.add(new String[]{tableName, tableComment});
                    }

                    // Fetch individual column information from the database as before
                    String selectColumnsQuery = "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, COLUMN_KEY, IS_NULLABLE, COLUMN_DEFAULT " +
                            "FROM information_schema.columns WHERE TABLE_SCHEMA = ?";
                    try (PreparedStatement columnsPreparedStatement = connection.prepareStatement(selectColumnsQuery)) {
                        columnsPreparedStatement.setString(1, database);

                        try (ResultSet columnsResultSet = columnsPreparedStatement.executeQuery()) {
                            Map<String, List<String[]>> tableDataMap = new HashMap<>();

                            while (columnsResultSet.next()) {
                                String tableName = columnsResultSet.getString("TABLE_NAME");
                                String columnName = columnsResultSet.getString("COLUMN_NAME");
                                String dataType = columnsResultSet.getString("DATA_TYPE");
                                String columnType = columnsResultSet.getString("COLUMN_TYPE");
                                String columnKey = columnsResultSet.getString("COLUMN_KEY");
                                String isNullable = columnsResultSet.getString("IS_NULLABLE");
                                String columnDefault = columnsResultSet.getString("COLUMN_DEFAULT");

                                String[] rowData = {columnName, dataType, isNullable, columnDefault, columnType};

                                tableDataMap.computeIfAbsent(tableName, k -> new ArrayList<>()).add(rowData);
                            }

                            // Convert the tableDataMap to the desired format (3D array)
                            String[][][] tableData = new String[tableDataMap.size() + 1][][];
                            int i = 0;

                            // Add the new table data (table names and comments)
                            String[][] tableInfoData = new String[tableInfoList.size() + 1][2];
                            tableInfoData[0] = new String[]{"Table ใน " + database};

                            String[] headerAllTable = {"COLUMN", "TYPE", "NULL", "DEFAULT", "COMMENT"};
                            tableInfoData[1] = headerAllTable;
                            
                            for (int j = 2; j < tableInfoList.size(); j++) {
                                tableInfoData[j] = tableInfoList.get(j);
                            }
                            tableData[i++] = tableInfoData;

                            // Add the existing individual column information
                            for (String tableName : tableDataMap.keySet()) {
                                List<String[]> rowDataList = tableDataMap.get(tableName);
                                int numRows = rowDataList.size() + 2;
                                int numCols = rowDataList.get(0).length;

                                tableData[i] = new String[numRows][numCols];

                                tableData[i][0][0] = "Table : "+ tableName;

                                String[] headers = {"COLUMN", "TYPE", "NULL", "DEFAULT", "COMMENT"};
                                tableData[i][1] = headers;

                                for (int j = 0; j < rowDataList.size(); j++) {
                                    tableData[i][j + 2] = rowDataList.get(j);
                                }

                                i++;
                            }

                            createDocx.createDocument(tableData);
                        }
                    }
                }
            }

            return "success";
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "error";
    }



}
