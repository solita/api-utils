package fi.solita.utils.api.format;

import fi.solita.utils.api.base.Cells;
import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.base.csv.CsvModule;
import fi.solita.utils.api.base.csv.CsvSerializers;
import fi.solita.utils.api.format.CsvConversionService;

public class CsvConversionServiceTest extends SpreadsheetConversionServiceTestBase {

    static final CsvModule module = new CsvModule(new CsvSerializers(new Serializers()).serializers());
    static final CsvConversionService service = new CsvConversionService(module);
    
    @Override
    protected Cells<?> serialize(Object o) {
        return module.serialize(o);
    }
}
