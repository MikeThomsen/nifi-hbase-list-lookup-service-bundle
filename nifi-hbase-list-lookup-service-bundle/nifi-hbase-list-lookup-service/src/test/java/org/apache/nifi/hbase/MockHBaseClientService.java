/*
 * Sections of this code derived from code in the Apache NiFi code base.
 */
package org.apache.nifi.hbase;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.hadoop.KerberosProperties;
import org.apache.nifi.hbase.put.PutColumn;
import org.apache.nifi.hbase.put.PutFlowFile;
import org.apache.nifi.hbase.scan.Column;
import org.apache.nifi.hbase.scan.ResultCell;
import org.apache.nifi.hbase.scan.ResultHandler;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Override methods to create a mock service that can return staged data
 */
public class MockHBaseClientService extends AbstractControllerService implements HBaseClientService {

    private Table table;
    private String family;
    private List<Result> results = new ArrayList<>();
    private KerberosProperties kerberosProperties;

    public MockHBaseClientService(final Table table, final String family, final KerberosProperties kerberosProperties) {
        this.table = table;
        this.family = family;
        this.kerberosProperties = kerberosProperties;
    }


    protected void setKerberosProperties(KerberosProperties properties) {
        this.kerberosProperties = properties;

    }

    public void addResult(final String rowKey, final Map<String, String> cells, final long timestamp) {
        final byte[] rowArray = rowKey.getBytes(StandardCharsets.UTF_8);
        final Cell[] cellArray = new Cell[cells.size()];
        int i = 0;
        for (final Map.Entry<String, String> cellEntry : cells.entrySet()) {
            final Cell cell = Mockito.mock(Cell.class);
            when(cell.getRowArray()).thenReturn(rowArray);
            when(cell.getRowOffset()).thenReturn(0);
            when(cell.getRowLength()).thenReturn((short) rowArray.length);

            final String cellValue = cellEntry.getValue();
            final byte[] valueArray = cellValue.getBytes(StandardCharsets.UTF_8);
            when(cell.getValueArray()).thenReturn(valueArray);
            when(cell.getValueOffset()).thenReturn(0);
            when(cell.getValueLength()).thenReturn(valueArray.length);

            final byte[] familyArray = family.getBytes(StandardCharsets.UTF_8);
            when(cell.getFamilyArray()).thenReturn(familyArray);
            when(cell.getFamilyOffset()).thenReturn(0);
            when(cell.getFamilyLength()).thenReturn((byte) familyArray.length);

            final String qualifier = cellEntry.getKey();
            final byte[] qualifierArray = qualifier.getBytes(StandardCharsets.UTF_8);
            when(cell.getQualifierArray()).thenReturn(qualifierArray);
            when(cell.getQualifierOffset()).thenReturn(0);
            when(cell.getQualifierLength()).thenReturn(qualifierArray.length);

            when(cell.getTimestamp()).thenReturn(timestamp);

            cellArray[i++] = cell;
        }

        final Result result = Mockito.mock(Result.class);
        when(result.getRow()).thenReturn(rowArray);
        when(result.rawCells()).thenReturn(cellArray);
        results.add(result);
    }

    @Override
    public void put(String s, Collection<PutFlowFile> collection) throws IOException {

    }

    @Override
    public void put(final String tableName, final byte[] rowId, final Collection<PutColumn> columns) throws IOException {
        Put put = new Put(rowId);
        Map<String, String> map = new HashMap<String, String>();
        for (final PutColumn column : columns) {
            put.addColumn(
                    column.getColumnFamily(),
                    column.getColumnQualifier(),
                    column.getBuffer());
            map.put(new String(column.getColumnQualifier()), new String(column.getBuffer()));
        }

        table.put(put);
        addResult(new String(rowId), map, 1);
    }

    @Override
    public boolean checkAndPut(final String tableName, final byte[] rowId, final byte[] family, final byte[] qualifier, final byte[] value, final PutColumn column) throws IOException {
        for (Result result : results) {
            if (Arrays.equals(result.getRow(), rowId)) {
                Cell[] cellArray = result.rawCells();
                for (Cell cell : cellArray) {
                    if (Arrays.equals(cell.getFamilyArray(), family) && Arrays.equals(cell.getQualifierArray(), qualifier)) {
                         if (value == null || Arrays.equals(cell.getValueArray(), value)) {
                             return false;
                         }
                    }
                }
            }
        }

        final List<PutColumn> putColumns = new ArrayList<PutColumn>();
        putColumns.add(column);
        put(tableName, rowId, putColumns);
        return true;
    }

    @Override
    public void delete(String s, byte[] bytes) throws IOException {

    }

    @Override
    public void delete(String s, byte[] bytes, String s1) throws IOException {

    }

    @Override
    public void delete(String s, List<byte[]> list) throws IOException {

    }

    @Override
    public void deleteCells(String s, List<DeleteRequest> list) throws IOException {

    }

    @Override
    public void delete(String s, List<byte[]> list, String s1) throws IOException {

    }

    @Override
    public void scan(String s, Collection<Column> collection, String s1, long l, ResultHandler resultHandler) throws IOException {

    }

    @Override
    public void scan(String s, Collection<Column> collection, String s1, long l, List<String> list, ResultHandler resultHandler) throws IOException {

    }

    private ResultCell getResultCell(Cell cell) {
        final ResultCell resultCell = new ResultCell();
        resultCell.setRowArray(cell.getRowArray());
        resultCell.setRowOffset(cell.getRowOffset());
        resultCell.setRowLength(cell.getRowLength());

        resultCell.setFamilyArray(cell.getFamilyArray());
        resultCell.setFamilyOffset(cell.getFamilyOffset());
        resultCell.setFamilyLength(cell.getFamilyLength());

        resultCell.setQualifierArray(cell.getQualifierArray());
        resultCell.setQualifierOffset(cell.getQualifierOffset());
        resultCell.setQualifierLength(cell.getQualifierLength());

        resultCell.setTimestamp(cell.getTimestamp());
        resultCell.setTypeByte(cell.getTypeByte());
        resultCell.setSequenceId(cell.getSequenceId());

        resultCell.setValueArray(cell.getValueArray());
        resultCell.setValueOffset(cell.getValueOffset());
        resultCell.setValueLength(cell.getValueLength());

        resultCell.setTagsArray(cell.getTagsArray());
        resultCell.setTagsOffset(cell.getTagsOffset());
        resultCell.setTagsLength(cell.getTagsLength());
        return resultCell;
    }

    @Override
    public void scan(String tableName, byte[] startRow, byte[] endRow, Collection<Column> columns, List<String> list, ResultHandler handler) throws IOException {
        try (final Table table = connection.getTable(TableName.valueOf(tableName));
             final ResultScanner scanner = getResults(table, startRow, endRow, columns, null)) {

            for (final Result result : scanner) {
                final byte[] rowKey = result.getRow();
                final Cell[] cells = result.rawCells();

                if (cells == null) {
                    continue;
                }

                // convert HBase cells to NiFi cells
                final ResultCell[] resultCells = new ResultCell[cells.length];
                for (int i=0; i < cells.length; i++) {
                    final Cell cell = cells[i];
                    final ResultCell resultCell = getResultCell(cell);
                    resultCells[i] = resultCell;
                }

                // delegate to the handler
                handler.handle(rowKey, resultCells);
            }
        }
    }

    @Override
    public void scan(String s, String s1, String s2, String s3, Long aLong, Long aLong1, Integer integer, Boolean aBoolean, Collection<Column> collection, List<String> list, ResultHandler resultHandler) throws IOException {

    }

    @Override
    public byte[] toBytes(boolean b) {
        return new byte[0];
    }

    @Override
    public byte[] toBytes(float v) {
        return new byte[0];
    }

    @Override
    public byte[] toBytes(int i) {
        return new byte[0];
    }

    @Override
    public byte[] toBytes(long l) {
        return new byte[0];
    }

    @Override
    public byte[] toBytes(double v) {
        return new byte[0];
    }

    @Override
    public byte[] toBytes(String s) {
        return new byte[0];
    }

    @Override
    public byte[] toBytesBinary(String s) {
        return new byte[0];
    }

    protected ResultScanner getResults(Table table, byte[] startRow, byte[] endRow, Collection<Column> columns, List<String> labels) throws IOException {
        final ResultScanner scanner = Mockito.mock(ResultScanner.class);
        Mockito.when(scanner.iterator()).thenReturn(results.iterator());
        return scanner;
    }

    protected ResultScanner getResults(Table table, Collection<Column> columns, Filter filter, long minTime, List<String> labels) throws IOException {
        final ResultScanner scanner = Mockito.mock(ResultScanner.class);
        Mockito.when(scanner.iterator()).thenReturn(results.iterator());
        return scanner;
    }

    protected ResultScanner getResults(final Table table, final String startRow, final String endRow, final String filterExpression, final Long timerangeMin, final Long timerangeMax,
            final Integer limitRows, final Boolean isReversed, final Collection<Column> columns)  throws IOException {
        final ResultScanner scanner = Mockito.mock(ResultScanner.class);
        Mockito.when(scanner.iterator()).thenReturn(results.iterator());
        return scanner;
    }

    protected Connection createConnection(ConfigurationContext context) throws IOException {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getTable(table.getName())).thenReturn(table);
        return connection;
    }

    private Connection connection;

    @OnEnabled
    public void onEnabled(ConfigurationContext context) throws IOException {
        connection = createConnection(context);
    }
}
