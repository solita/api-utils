package fi.solita.utils.api.format;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fi.solita.utils.api.base.Cells;
import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.base.excel.ExcelModule;
import fi.solita.utils.api.base.excel.ExcelSerializers;
import fi.solita.utils.api.format.ExcelConversionService;

public class ExcelConversionServiceTest extends SpreadsheetConversionServiceTestBase {

    static final ExcelModule module = new ExcelModule(new ExcelSerializers(new Serializers()).serializers());
    static final ExcelConversionService service = new ExcelConversionService(module);
    
    @Override
    protected Cells<?> serialize(Object o) {
        return module.serialize(new XSSFWorkbook().createSheet().createRow(0), 0, o);
    }
}
