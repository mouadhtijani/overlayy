package eec.epi.scripts.compta;

import eec.epi.scripts.JobScript;
import eec.epi.scripts.ServiceAble;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.AccountingCode;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoiceLine;
import org.meveo.model.billing.Subscription;
import org.meveo.model.crm.Customer;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.OperationCategoryEnum;
import org.meveo.service.admin.impl.SellerService;
import org.meveo.service.billing.impl.AccountingCodeService;
import org.meveo.service.billing.impl.InvoiceTypeService;
import org.meveo.service.custom.CustomTableService;
import org.meveo.service.payments.impl.AccountOperationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CT_FAN350P1_Alimentation extends JobScript {

    // Define the AccountOperationService and SellerService instances.
    private final AccountOperationService accountOperationService = getServiceInterface(AccountOperationService.class);
    private final SellerService sellerService = getServiceInterface(SellerService.class);

    // Define constants for custom fields.
    private static final String CF_NUMERO_PIECE_COMPTABLE = "cf_numero_piece_comptable";
    private static final String CF_AO_EXPORT_COMPTA = "cf_ao_export_compta";
    List<Map<String,String>> errorLog = new ArrayList<>();
    // The main execution method.
    @Override
    public void execute(JobContext jobContext) {
        // Get a list of filtered account operations.
        List<AccountOperation> accountOperations = getFilteredAccountOperations();

        // Check if the list is empty.
        if (accountOperations.isEmpty()) {
            jobContext.addKO();
            // If empty, report a failure and exit.
            jobContext.addReport("KO : MISSINGAO_ALIM_CT_FAN350P1 Aucun account operation n'est sélectionné pour la génération d’une écriture comptable dans la CT_FAN350P1");
            return;
        }

        // Iterate through each account operation.
        for (AccountOperation ao : accountOperations) {
            int dnolig = 1;
            CTLine ctLine = new CTLine();
            Invoice invoice = ctLine.getBillingInvoice(ao);
            if (invoice == null) {
                addError(ao.getId(),"invoice");
                jobContext.addKO();
                continue;
            }
            List<Map<String, Object>> ctExploitations = ctLine.getCtExploitations(invoice);
            List<InvoiceLine> invoiceLines = invoice.getInvoiceLines();
            List<Map<String, Object>> valuesList = new ArrayList<>();

            // Check if required data is missing.
            if (ctExploitations == null || ctExploitations.isEmpty() || invoiceLines == null || invoiceLines.isEmpty()) {
                addError(ao.getId(),"ctExploitations or invoiceLines are null");
                jobContext.addKO();
                continue;
            }

            // Initialize CTLine for account operation and invoice.
            if (ctLine.initCTLine(ao, invoice, ctExploitations, ctLine) == null) {
                jobContext.addKO();
                continue;
            }

            // Fill client records.
            if (ctLine.fillClientRecord(ctLine, ao, invoice, ctExploitations) == null) {
                jobContext.addKO();
                continue;
            }

            dnolig = ctLine.updateDnolig(dnolig);
            valuesList.add(ctLine.prepareCTLine());

            // Calculate and process sums for article-related lines.
            Map<String, Map<String, BigDecimal>> codeAndArticleToSumOfAmountWithoutTax = calculateArticleSums(invoiceLines);
            if(codeAndArticleToSumOfAmountWithoutTax == null || codeAndArticleToSumOfAmountWithoutTax.isEmpty()){
                addError(ao.getId(),"IL.Dmont ");
                jobContext.addKO();
                continue;
            }
            dnolig = processArticleSummaries(codeAndArticleToSumOfAmountWithoutTax, ao, invoice, ctExploitations, dnolig, valuesList, jobContext);

            if(dnolig == -1 ){
                jobContext.addKO();
                continue;
            }
            // Calculate and process sums for tax-related lines.
            Map<String, BigDecimal> codeToSumOfAmountTax = calculateTaxSums(invoiceLines);
            if(codeToSumOfAmountTax == null || codeToSumOfAmountTax.isEmpty()){
                addError(ao.getId(),"TGC.Dmont");
                jobContext.addKO();
                continue;
            }

            dnolig = processTaxSummaries(codeToSumOfAmountTax, ao, invoice, ctExploitations, dnolig, valuesList, jobContext);

            if(dnolig == -1) {
                jobContext.addKO();
                continue;
            }
            // Reverse the list and save the CT lines.
            Collections.reverse(valuesList);
            ctLine.saveCTlines(valuesList);

            // Update custom fields in AccountOperation and Seller.
            updateCfNumPiece(ao);
            updateCfAoExportCompta(ao);
            jobContext.addOK();

        }
        // Report an error if necessary data is missing.
        if(jobContext.nbKO() > 0){
            jobContext.addReport("KO : MISSINGDATA_ALIM_CT_FAN350P1 "+jobContext.nbKO() + " AO en erreur. Une ou plusieurs données sont manquantes ou n'ont pas pu être récupérées pour alimenter la CT_FAN350P1");
            jobContext.addReport(generateError());
        }
        if(jobContext.nbOK() > 0)
            jobContext.addReport( "OK : SUCCESS_ALIM_CT_FAN350P1 "+jobContext.nbOK()+" Accounts Operations alimentés dans la table CT_FAN350P1 ");
    }

    // Helper method to update custom field CF_AO_EXPORT_COMPTA in AccountOperation.
    protected void updateCfAoExportCompta(AccountOperation ao) {
        ao.setCfValue(CF_AO_EXPORT_COMPTA, new Date());
        accountOperationService.update(ao);
    }

    // Helper method to update custom field CF_NUMÉRO_PIÉCE_COMPTABLE in Seller.
    protected void updateCfNumPiece(AccountOperation ao) {
        Seller s = ao.getSeller();
        Object values = Integer.parseInt(s.getCfValue(CF_NUMERO_PIECE_COMPTABLE).toString()) + 1;
        s.setCfValue(CF_NUMERO_PIECE_COMPTABLE, values);
        sellerService.update(s);
    }

    // Helper method to get a list of filtered account operations.
    protected List<AccountOperation> getFilteredAccountOperations() {
        Date currentDate = new Date();
        String hqlQuery = "SELECT ct FROM AccountOperation ct WHERE ct.type = 'I' AND ct.transactionDate < :currentDate AND (ct.cfValues IS NULL OR function('jsonb_extract_path_text', ct.cfValues, 'cf_ao_export_compta') IS NULL)";
        Map<String, Object> param = new HashMap<>();
        param.put("currentDate", currentDate);
        return (List<AccountOperation>) accountOperationService.executeSelectQuery(hqlQuery, param);
    }

    // Helper method to calculate sums for articles.
    protected Map<String, Map<String, BigDecimal>> calculateArticleSums(List<InvoiceLine> invoiceLines) {
        return invoiceLines.stream()
                .filter(line -> line.getAccountingArticle() != null && line.getAccountingArticle().getAccountingCode() != null && line.getAccountingArticle().getAccountingCode() != null )
                .collect(Collectors.groupingBy(
                        line -> line.getAccountingArticle().getAccountingCode().getCode(),
                        Collectors.groupingBy(
                                line -> line.getAccountingArticle().getCode(),
                                Collectors.mapping(InvoiceLine::getAmountWithoutTax, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                        )
                ));
    }

    // Helper method to process article summaries.
    protected int processArticleSummaries(Map<String, Map<String, BigDecimal>> codeAndArticleToSumOfAmountWithoutTax, AccountOperation ao, Invoice invoice, List<Map<String, Object>> ctExploitations, int dnolig, List<Map<String, Object>> valuesList, JobContext jobContext) {
        for (Map.Entry<String, Map<String, BigDecimal>> entry : codeAndArticleToSumOfAmountWithoutTax.entrySet()) {
            String accountingCode = entry.getKey();
            Map<String, BigDecimal> articleToSumOfAmountWithoutTax = entry.getValue();
            String articleCode = "";
            BigDecimal sumOfAmountWithoutTax = BigDecimal.ZERO;

            for (Map.Entry<String, BigDecimal> articleEntry : articleToSumOfAmountWithoutTax.entrySet()) {
                articleCode = articleEntry.getKey();
                sumOfAmountWithoutTax = sumOfAmountWithoutTax.add(articleEntry.getValue());
            }

            CTLine ctLineInvoiceLine = new CTLine();

            if (ctLineInvoiceLine.initCTLine(ao, invoice, ctExploitations, ctLineInvoiceLine) == null) {
                return -1;
            }

            if (ctLineInvoiceLine.fillInvoiceLinerecords(ctLineInvoiceLine, invoice, articleCode, accountingCode, sumOfAmountWithoutTax.abs(), ao) == null) {
                return -1;
            }

            dnolig = ctLineInvoiceLine.updateDnolig(dnolig);
            valuesList.add(ctLineInvoiceLine.prepareCTLine());
        }
        return dnolig;
    }
    private int processTaxSummaries(Map<String, BigDecimal> codeToSumOfAmountTax, AccountOperation ao, Invoice invoice, List<Map<String, Object>> ctExploitations, int dnolig, List<Map<String, Object>> valuesList, JobContext jobContext) {
        for (String code : codeToSumOfAmountTax.keySet()) {
            BigDecimal sumOfAmountTax = codeToSumOfAmountTax.get(code).abs();
            CTLine ctLineTGC = new CTLine();

            if (ctLineTGC.initCTLine(ao, invoice, ctExploitations, ctLineTGC) == null) {
                return -1;
            }

            if (ctLineTGC.fillTGCrecords(ctLineTGC, invoice, ctExploitations, code, sumOfAmountTax, ao) == null) {
                return -1;
            }

            dnolig = ctLineTGC.updateDnolig(dnolig);
            valuesList.add(ctLineTGC.prepareCTLine());
        }
        return dnolig;
    }

    // Helper method to calculate sums for taxes.
    protected Map<String, BigDecimal> calculateTaxSums(List<InvoiceLine> invoiceLines) {
        return invoiceLines.stream()
                .filter(line -> line.getTax() != null && line.getTax().getAccountingCode() != null)
                .collect(Collectors.groupingBy(
                        line -> line.getTax().getAccountingCode().getCode(),
                        Collectors.mapping(InvoiceLine::getAmountTax, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
    }


    class CTLine implements ServiceAble {

        private final CustomTableService customTableService = getServiceInterface(CustomTableService.class);
        private final InvoiceTypeService invoiceTableService = getServiceInterface(InvoiceTypeService.class);
        private final AccountingCodeService accountingCodeService = getServiceInterface(AccountingCodeService.class);

        // declare table name
        String TAB_FAN350P1 = "CT_FAN350P1";
        String TAB_ct_exploitations =  "ct_exploitations";
        private static final String CF_CODE_EXPLOITATION_SUB ="cf_code_exploitation";
        private static final String CF_NUMÉRO_PIÉCE_COMPTABLE ="cf_numero_piece_comptable";
        private static final String CF_CODE_EXPLOITATION ="code_exploitation";

        private static final String CF_CODE_JOURNAL ="code_journal";
        private static final String CF_CODE_EXPLOITATION_ANAEL = "code_exploitation_anael";

        // declare fields name
        private static final String RTYPE_NAME = "rtype";
        private static final String RSTE_NAME = "rste";
        private static final String RETAB_NAME = "retab";
        private static final String RGEN_NAME = "rgen";
        private static final String RAUX_NAME = "raux";
        private static final String REX_NAME = "rex";
        private static final String RPERIO_NAME = "rperio";
        private static final String RPERIG_NAME = "rperig";
        private static final String RPERIA_NAME = "rperia";
        private static final String RANP_NAME = "ranp";
        private static final String RMOISP_NAME = "rmoisp";
        private static final String RJOURP_NAME = "rjourp";
        private static final String RCE_NAME = "rce";
        private static final String DEX_NAME = "dex";
        private static final String DDATOA_NAME = "ddatoa";
        private static final String DDATOM_NAME = "ddatom";
        private static final String DDATOJ_NAME = "ddatoj";
        private static final String dchron_name = "dchron";
        private static final String DJAL_NAME = "djal";
        private static final String DPIECE_NAME = "dpiece";
        private static final String DTYPPI_NAME = "dtyppi";
        private static final String DLIB_NAME = "dlib";
        private static final String DMONT_NAME = "dmont";
        private static final String DCOEF_NAME = "dcoef";
        private static final String DMDEV_NAME = "dmdev";
        private static final String DECHA_NAME = "decha";
        private static final String DECHM_NAME = "dechm";
        private static final String DECHJ_NAME = "dechj";
        private static final String DTYPRM_NAME = "dtyprm";
        private static final String DVALA_NAME = "dvala";
        private static final String DVALM_NAME = "dvalm";
        private static final String DVALJ_NAME = "dvalj";
        private static final String DSTA_1_NAME = "dsta1";
        private static final String DSTA_2_NAME = "dsta2";
        private static final String DTVA_NAME = "dtva";
        private static final String DBORD_NAME = "dbord";
        private static final String DCONTR_NAME = "dcontr";
        private static final String DMAT_1_NAME = "dmat1";
        private static final String DMAT_2_NAME = "dmat2";
        private static final String DMAT_1_B_NAME = "dmat1b";
        private static final String DMAT_2_B_NAME = "dmat2b";
        private static final String DVT_NAME = "dvt";
        private static final String DOB_NAME = "dob";
        private static final String DTX_NAME = "dtx";
        private static final String DLET_NAME = "dlet";
        private static final String DLIT_NAME = "dlit";
        private static final String DFOLIO_NAME = "dfolio";
        private static final String DWS_NAME = "dws";
        private static final String DINT_NAME = "dint";
        private static final String DNOLIG_NAME = "dnolig";
        private static final String DREGLT_NAME = "dreglt";
        private static final String DAFFEC_NAME = "daffec";
        private static final String DAFFE_2_NAME = "daffe2";
        private static final String DAFFE_3_NAME = "daffe3";
        private static final String DIDENT_NAME = "dident";
        private static final String DCDEV_NAME = "dcdev";
        private static final String DLBQE_NAME = "dlbqe";
        private static final String DSTA_15_NAME = "dsta15";
        private static final String DATGSA_NAME = "datgsa";
        private static final String DATGSM_NAME = "datgsm";
        private static final String DATGSJ_NAME = "datgsj";
        private static final String DREL_NAME = "drel";
        private static final String DNBREL_NAME = "dnbrel";
        private static final String DTRELA_NAME = "dtrela";
        private static final String DTRELM_NAME = "dtrelm";
        private static final String DTRELJ_NAME = "dtrelj";
        private static final String DTHLET_NAME = "dthlet";
        private static final String DATLET_NAME = "datlet";
        private static final String DSVECH_NAME = "dsvech";
        private static final String DNBECH_NAME = "dnbech";
        private static final String DSVREG_NAME = "dsvreg";
        private static final String DSVPER_NAME = "dsvper";
        private static final String DSGEN_NAME = "dsgen";
        private static final String DT_01_NAME = "dt01";
        private static final String DT_02_NAME = "dt02";
        private static final String DT_03_NAME = "dt03";
        private static final String DT_04_NAME = "dt04";
        private static final String DT_05_NAME = "dt05";
        private static final String DT_06_NAME = "dt06";
        private static final String DT_07_NAME = "dt07";
        private static final String DT_08_NAME = "dt08";
        private static final String DT_09_NAME = "dt09";
        private static final String DT_10_NAME = "dt10";
        private static final String DSTA_20_NAME = "dsta20";
        private static final String RSECT_NAME = "rsect";
        private static final String RCHAP_NAME = "rchap";
        private static final String RNAT_NAME = "rnat";
        private static final String RSYMB_NAME = "rsymb";
        private static final String RART_NAME = "rart";
        private static final String AQTE_NAME = "aqte";
        private static final String ASTD_NAME = "astd";
        private static final String AUNCOT_NAME = "auncot";
        private static final String AMAT_1_NAME = "amat1";
        private static final String AMAT_2_NAME = "amat2";
        private static final String ATHLET_NAME = "athlet";
        private static final String AATLET_NAME = "aatlet";
        private static final String ALET_NAME = "alet";
        private static final String AT_01_NAME = "at01";
        private static final String AT_02_NAME = "at02";
        private static final String AT_03_NAME = "at03";
        private static final String AT_04_NAME = "at04";
        private static final String AT_05_NAME = "at05";
        private static final String AT_06_NAME = "at06";
        private static final String AT_07_NAME = "at07";
        private static final String AT_08_NAME = "at08";
        private static final String AT_09_NAME = "at09";
        private static final String AT_10_NAME = "at10";
        private static final String DLIB_1_NAME = "dlib1";
        private static final String DLIB_2_NAME = "dlib2";
        private static final String DLIB_3_NAME = "dlib3";
        private static final String DLIB_4_NAME = "dlib4";
        private static final String DBQE_NAME = "dbqe";
        private static final String DGUI_NAME = "dgui";
        private static final String DCPTE_NAME = "dcpte";
        private static final String DRIB_NAME = "drib";
        private static final String DRELVA_NAME = "drelva";
        private static final String DRELVM_NAME = "drelvm";
        private static final String DRELVJ_NAME = "drelvj";
        private static final String DNREL_NAME = "dnrel";
        private static final String DRBQA_NAME = "drbqa";
        private static final String DRBQM_NAME = "drbqm";
        private static final String DRBQJ_NAME = "drbqj";
        private static final String DNBAN_NAME = "dnban";
        private static final String DENTRE_NAME = "dentre";
        private static final String DREFT_NAME = "dreft";
        private static final String DCODE_NAME = "dcode";
        private static final String DRCE_NAME = "drce";
        private static final String DER_NAME = "der";
        private static final String DERPTF_NAME = "derptf";
        private static final String RDEBMJ_NAME = "rdebmj";
        private static final String RFINMJ_NAME = "rfinmj";
        private static final String RSECT_2_NAME = "rsect2";
        private static final String RCHAP_2_NAME = "rchap2";
        private static final String RNAT_2_NAME = "rnat2";
        private static final String RSECT_3_NAME = "rsect3";
        private static final String RCHAP_3_NAME = "rchap3";
        private static final String RNAT_3_NAME = "rnat3";
        private static final String RSECT_4_NAME = "rsect4";
        private static final String RCHAP_4_NAME = "rchap4";
        private static final String RNAT_4_NAME = "rnat4";
        private static final String RSECT_5_NAME = "rsect5";
        private static final String RCHAP_5_NAME = "rchap5";
        private static final String RNAT_5_NAME = "rnat5";
        private static final String RSECT_6_NAME = "rsect6";
        private static final String RCHAP_6_NAME = "rchap6";
        private static final String RNAT_6_NAME = "rnat6";
        private static final String RSECT_7_NAME = "rsect7";
        private static final String RCHAP_7_NAME = "rchap7";
        private static final String RNAT_7_NAME = "rnat7";
        private static final String RSECT_8_NAME = "rsect8";
        private static final String RCHAP_8_NAME = "rchap8";
        private static final String RNAT_8_NAME = "rnat8";
        private static final String RSECT_9_NAME = "rsect9";
        private static final String RCHAP_9_NAME = "rchap9";
        private static final String RNAT_9_NAME = "rnat9";
        private static final String LIGANA_NAME = "ligana";
        private static final String ASTA_36_NAME = "asta36";
        private static final String DERS_NAME = "ders";

        // declare variables
        protected String rtype = "";
        protected String rste = "";
        protected String retab = "";
        protected String rgen = "";
        protected String raux = "";
        protected String rex = "";
        protected String rperio = "";
        protected String rperig = "";
        protected String rperia = "";
        protected String ranp = "";
        protected String rmoisp = "";
        protected String rjourp = "";
        protected String rce = "";
        protected String dex = "";
        protected String ddatoa = "";
        protected String ddatom = "";
        protected String ddatoj = "";
        protected String dchron = "";
        protected String djal = "";
        protected String dpiece = "";
        protected String dtyppi = "";
        protected String dlib = "";
        protected String dmont = "";
        protected String dcoef = "";
        protected String dmdev = "";
        protected String decha = "";
        protected String dechm = "";
        protected String dechj = "";
        protected String dtyprm = "";
        protected String dvala = "";
        protected String dvalm = "";
        protected String dvalj = "";
        protected String dsta1 = "";
        protected String dsta2 = "";
        protected String dtva = "";
        protected String dbord = "";
        protected String dcontr = "";
        protected String dmat1 = "";
        protected String dmat2 = "";
        protected String dmat1b = "";
        protected String dmat2b = "";
        protected String dvt = "";
        protected String dob = "";
        protected String dtx = "";
        protected String dlet = "";
        protected String dlit = "";
        protected String dfolio = "";
        protected String dws = "";
        protected String dint = "";
        protected String dnolig = "";
        protected String dreglt = "";
        protected String daffec = "";
        protected String daffe2 = "";
        protected String daffe3 = "";
        protected String dident = "";
        protected String dcdev = "";
        protected String dlbqe = "";
        protected String dsta15 = "";
        protected String datgsa = "";
        protected String datgsm = "";
        protected String datgsj = "";
        protected String drel = "";
        protected String dnbrel = "";
        protected String dtrela = "";
        protected String dtrelm = "";
        protected String dtrelj = "";
        protected String dthlet = "";
        protected String datlet = "";
        protected String dsvech = "";
        protected String dnbech = "";
        protected String dsvreg = "";
        protected String dsvper = "";
        protected String dsgen = "";
        protected String dt01 = "";
        protected String dt02 = "";
        protected String dt03 = "";
        protected String dt04 = "";
        protected String dt05 = "";
        protected String dt06 = "";
        protected String dt07 = "";
        protected String dt08 = "";
        protected String dt09 = "";
        protected String dt10 = "";
        protected String dsta20 = "";
        protected String rsect = "";
        protected String rchap = "";
        protected String rnat = "";
        protected String rsymb = "";
        protected String rart = "";
        protected String aqte = "";
        protected String astd = "";
        protected String auncot = "";
        protected String amat1 = "";
        protected String amat2 = "";
        protected String athlet = "";
        protected String aatlet = "";
        protected String alet = "";
        protected String at01 = "";
        protected String at02 = "";
        protected String at03 = "";
        protected String at04 = "";
        protected String at05 = "";
        protected String at06 = "";
        protected String at07 = "";
        protected String at08 = "";
        protected String at09 = "";
        protected String at10 = "";
        protected String dlib1 = "";
        protected String dlib2 = "";
        protected String dlib3 = "";
        protected String dlib4 = "";
        protected String dbqe = "";
        protected String dgui = "";
        protected String dcpte = "";
        protected String drib = "";
        protected String drelva = "";
        protected String drelvm = "";
        protected String drelvj = "";
        protected String dnrel = "";
        protected String drbqa = "";
        protected String drbqm = "";
        protected String drbqj = "";
        protected String dnban = "";
        protected String dentre = "";
        protected String dreft = "";
        protected String dcode = "";
        protected String drce = "";
        protected String der = "";
        protected String derptf = "";
        protected String rdebmj = "";
        protected String rfinmj = "";
        protected String rsect2 = "";
        protected String rchap2 = "";
        protected String rnat2 = "";
        protected String rsect3 = "";
        protected String rchap3 = "";
        protected String rnat3 = "";
        protected String rsect4 = "";
        protected String rchap4 = "";
        protected String rnat4 = "";
        protected String rsect5 = "";
        protected String rchap5 = "";
        protected String rnat5 = "";
        protected String rsect6 = "";
        protected String rchap6 = "";
        protected String rnat6 = "";
        protected String rsect7 = "";
        protected String rchap7 = "";
        protected String rnat7 = "";
        protected String rsect8 = "";
        protected String rchap8 = "";
        protected String rnat8 = "";
        protected String rsect9 = "";
        protected String rchap9 = "";
        protected String rnat9 = "";
        protected String ligana = "";
        protected String asta36 = "";
        protected String ders = "";

        public CTLine() {
        }
        protected String getYYofCurrentYear(){
            Year currentYear = Year.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy");
            return currentYear.format(formatter);
        }
        protected String getDegitsFromDate(Date date, String choice){
            Instant instant;
            if (date instanceof java.sql.Date) {
                Date utilDate = new Date(date.getTime());
                instant = utilDate.toInstant();
            }
            else {
                instant = date.toInstant();
            }

            // Convert java.time.Instant to java.time.LocalDate
            LocalDate localDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            switch (choice) {
                case "d":
                    return localDate.getDayOfMonth()+"";   // Day
                case "m":
                    return localDate.getMonthValue()+""; // Month
                case "y":
                    return localDate.getYear() % 100+"";  // 2 degits Year
                case "fy":
                    return localDate.getYear()+"";  // 4 degits Year
                default:
                    throw new IllegalArgumentException("Invalid choice: " + choice);
            }

        }
        public Invoice getBillingInvoice(AccountOperation ao) {
            if(ao.getReference() == null || ao.getReference().isEmpty()) return null;

            String hqlQuery = "SELECT inv FROM Invoice inv WHERE inv.invoiceNumber = :reference";
            Map<String,Object> param = new HashMap<>();
            param.put("reference", ao.getReference());
            List<Invoice> result =    (List<Invoice>) invoiceTableService.executeSelectQuery(hqlQuery,param);
            if(result == null || result.isEmpty()) return null;
            return  result.get(0);
        }
        public List<Map<String, Object>> getCtExploitations(Invoice invoice) {
            if(invoice.getBillingAccount().getUsersAccounts() == null || invoice.getBillingAccount().getUsersAccounts().isEmpty()) return null;
            if(invoice.getBillingAccount().getUsersAccounts().get(0).getSubscriptions() == null || invoice.getBillingAccount().getUsersAccounts().get(0).getSubscriptions().isEmpty() ) return null;
            Subscription subscription = invoice.getBillingAccount().getUsersAccounts().get(0).getSubscriptions().get(0);
            Object cf_code_etab = subscription.getCfValue(CF_CODE_EXPLOITATION_SUB);
            if(cf_code_etab == null) return null;

            HashMap<String, Object> filters = new HashMap<>();
            filters.put(CF_CODE_EXPLOITATION, cf_code_etab.toString());

            return customTableService.list(TAB_ct_exploitations, new PaginationConfiguration(filters));
        }
        public String getCodeJournal(List<Map<String, Object>> ct_exploitations){
            Map<String,Object> ct_exploitation = ct_exploitations.get(0);
            Object ct_exp_journal_code = ct_exploitation.get(CF_CODE_JOURNAL);
            if(ct_exp_journal_code == null) return null;
            return ct_exp_journal_code.toString();
        }

//        public String getCodeExploitation(List<Map<String, Object>> ct_exploitations){
//            Map<String,Object> ct_exploitation = ct_exploitations.get(0);
//            Object ct_code_expoloitation = ct_exploitation.get(CF_CODE_EXPLOITATION);
//            if(ct_code_expoloitation == null) return null;
//            return ct_code_expoloitation.toString();
//        }
        public String getCodeExploitationANAEL(List<Map<String, Object>> ct_exploitations){
            Map<String,Object> ct_exploitation = ct_exploitations.get(0);
            Object ct_code_expoloitation = ct_exploitation.get(CF_CODE_EXPLOITATION_ANAEL);
            if(ct_code_expoloitation == null) return null;
            return ct_code_expoloitation.toString();
        }
        public String getSubscriptionCode(Invoice invoice) {

            if(invoice.getBillingAccount().getUsersAccounts() == null || invoice.getBillingAccount().getUsersAccounts().isEmpty()) return null;
            if(invoice.getBillingAccount().getUsersAccounts().get(0).getSubscriptions() == null || invoice.getBillingAccount().getUsersAccounts().get(0).getSubscriptions().isEmpty()) return null;
            String subCode = invoice.getBillingAccount().getUsersAccounts().get(0).getSubscriptions().get(0).getCode();
            if (subCode == null) return null;
            return subCode;
        }

        public String getDtvaTGC(Invoice invoice) {

            if(invoice.getBillingAccount().getUsersAccounts() == null || invoice.getBillingAccount().getUsersAccounts().isEmpty()) return null;
            if(invoice.getBillingAccount().getUsersAccounts().get(0).getSubscriptions() == null || invoice.getBillingAccount().getUsersAccounts().get(0).getSubscriptions().isEmpty()) return null;
            String offerCode = invoice.getBillingAccount().getUsersAccounts().get(0).getSubscriptions().get(0).getOffer().getCode();
            if (offerCode == null) return null;
            if (offerCode.equals("OFFER_BORNE_VE")) return "1";
            if (offerCode.equals("OFFER_BORNE_VE_HTA")) return "2";
            else if (offerCode.equals("OFFER_IRRIGATION")) return "E";
            return null;
        }
        protected String getRgenClientRecord(Invoice invoice) {
            if(invoice.getBillingAccount() == null) return null;
            CustomerAccount customerAccount = invoice.getBillingAccount().getCustomerAccount();
            if(customerAccount == null) return null;
            Customer customer = customerAccount.getCustomer();
            if(customer == null) return null;
            AccountingCode cfCategorieClient = customer.getCustomerCategory().getAccountingCode();
            if(cfCategorieClient == null) return null;
            return cfCategorieClient.getCode();
        }
        public CTLine fillTGCrecords(CTLine ctLine, Invoice invoice, List<Map<String, Object>> ct_exploitations, String code, BigDecimal sumOfAmountTax, AccountOperation ao) {
            String DtvaValue = getDtvaTGC(invoice);
            if (DtvaValue == null) {
                addError(ao.getId(),"TGC.Dtva");
                return null;
            }
            if (code == null) {
                addError(ao.getId(),"TGC.Rsect");
                return null;
            }
            if (sumOfAmountTax == null) {
                addError(ao.getId(),"TGC.Dmont");
                return null;
            }
            ctLine.setRgen(code);
            ctLine.setDmont(sumOfAmountTax.toString());
            ctLine.setDecha("0");
            ctLine.setDechm("0");
            ctLine.setDechj("0");
            ctLine.setDtva(DtvaValue);
            ctLine.setRnat("0");
            String dlibValue = getBillingAcountCodeDescription(ctLine.getRgenCode());
            if(dlibValue == null) {
                addError(ao.getId(),"TGC.Dlib");
                return null;
            }
            ctLine.setDlib(dlibValue);
            String coefValue = getCoef(ao,"lf");
            if(coefValue == null) {
                addError(ao.getId(),"TGC.Dcoef");
                return null;
            }
            ctLine.setDcoef(coefValue);

            return ctLine;
        }

        public String getAnalyticCode(Invoice invoice, String code, String filter) {
            Optional<String> analyticCode = invoice.getInvoiceLines().stream()
                    .filter(line -> line.getAccountingArticle() != null && code.equals(line.getAccountingArticle().getCode()))
                    .map(line -> getAnalyticCodeByFilter(line, filter))
                    .findFirst();

            return analyticCode.orElse(null); // Return an empty string if no code is found
        }

        private String getAnalyticCodeByFilter(InvoiceLine line, String filter) {
            switch (filter) {
                case "analyticCode1":
                    if (line.getAccountingArticle().getAnalyticCode1() == null) return "";
                    return line.getAccountingArticle().getAnalyticCode1();
                case "analyticCode2":
                    if (line.getAccountingArticle().getAnalyticCode2() == null) return "";
                    return line.getAccountingArticle().getAnalyticCode2();
                case "analyticCode3":
                    if (line.getAccountingArticle().getAnalyticCode3() == null) return "";
                    return line.getAccountingArticle().getAnalyticCode3();
                default:
                    // Handle the case where an invalid filter is provided
                    return "";
            }
        }
        public int updateDnolig(int dnolig){
            this.setDnolig(dnolig+"");
            return dnolig+1;
        }
        public CTLine fillInvoiceLinerecords(CTLine ctLine, Invoice invoice, String code, String accountingCode, BigDecimal sumOfAmountWithoutTax, AccountOperation ao) {
             String DtvaValue = getDtvaTGC(invoice);
            if (DtvaValue == null) {
                addError(ao.getId(),"IL.Dtva");
                return null;
            }
            if (code == null) {
                addError(ao.getId(),"IL.Rsect");
                return null;
            }

            if (accountingCode == null) {
                addError(ao.getId(),"IL.Rgen");
                return null;
            }

            if (sumOfAmountWithoutTax == null) {
                addError(ao.getId(),"IL.Dmont");
                return null;
            }

            ctLine.setRgen(accountingCode);
            ctLine.setDmont(sumOfAmountWithoutTax.toString());
            ctLine.setDecha("0");
            ctLine.setDechm("0");
            ctLine.setDechj("0");
            ctLine.setDtva(DtvaValue);
            String analyticCode1 = getAnalyticCode(invoice,code,"analyticCode1");
            if(analyticCode1 == null) {
                addError(ao.getId(),"IL.Rsect");
                return null;
            }
            ctLine.setRsect(analyticCode1);

            String analyticCode2 = getAnalyticCode(invoice,code,"analyticCode2");
            if(analyticCode2 == null) {
                addError(ao.getId(),"IL.Rchap");
                return null;
            }
            ctLine.setRchap(analyticCode2);

            String analyticCode3 = getAnalyticCode(invoice,code,"analyticCode3");
            if(analyticCode3 == null){
                addError(ao.getId(),"IL.Rnat");
                return null;
            }
            ctLine.setRnat(analyticCode3);
            String dlibValue = getBillingAcountCodeDescription(ctLine.getRgenCode());
            if(dlibValue == null) {
                addError(ao.getId(),"IL.Dlib");
                return null;
            }            ctLine.setDlib(dlibValue);
            String coefValue = getCoef(ao,"lf");
            if(coefValue == null) {
                addError(ao.getId(),"IL.Dcoef");
                return null;
            }
            ctLine.setDcoef(coefValue);

            return ctLine;
        }
        public String getSumOfAmount(Invoice invoice) {
            List<InvoiceLine> invoiceLines = invoice.getInvoiceLines();
            return   invoiceLines.stream()
                    .map(InvoiceLine::getAmountWithTax)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .abs()
                    .toString();
        }
        public CTLine fillClientRecord(CTLine ctLine, AccountOperation ao, Invoice invoice, List<Map<String, Object>> ct_exploitations) {

            String rauxValue = getSubscriptionCode(invoice);
            if(rauxValue == null){
                addError(ao.getId(),"C.Raux");
                return null;
            }
            ctLine.setRaux(rauxValue);
            String rgenValue = getRgenClientRecord(invoice);
            if(rgenValue == null) {
                addError(ao.getId(),"C.Rgen");
                return null;
            }
            ctLine.setRgen(rgenValue);
            if(invoice.getInvoiceNumber() == null){
                addError(ao.getId(),"C.Dpiece");
                return null;
            }
            ctLine.setDpiece(invoice.getInvoiceNumber());
            String dmont = getSumOfAmount(invoice);
            if( dmont == null) {
                addError(ao.getId(),"C.Dmont");
                return null;
            }
            ctLine.setDmont(dmont);
            if(ao.getDueDate() == null ){
                addError(ao.getId(),"C.Decha");
                return null;
            }
            ctLine.setDecha(getDegitsFromDate(ao.getDueDate(),"fy"));
            ctLine.setDechm(getDegitsFromDate(ao.getDueDate(),"m"));
            ctLine.setDechj(getDegitsFromDate(ao.getDueDate(),"d"));
             ctLine.setDnolig("1");
             ctLine.setRnat("0");
             String dlibValue = getBillingAcountCodeDescription(ctLine.getRgenCode());
            if(dlibValue == null) {
                addError(ao.getId(),"C.Dlib");
                return null;
            }
            ctLine.setDlib(dlibValue);
            String coefValue = getCoef(ao,"client");
            if(coefValue == null) {
                addError(ao.getId(),"C.Dcoef");
                return null;
            }
            ctLine.setDcoef(coefValue);
            return ctLine;
        }


        protected String getCoef(AccountOperation ao,String type) {
            if(ao.getTransactionCategory() == null) return null;

            if (type.equals("client"))  {
                if(ao.getTransactionCategory() == OperationCategoryEnum.DEBIT) return "1";
                return "-1";
            }
            else if (type.equals("lf")){
                if(ao.getAmountWithoutTax() == null) return null;
                if(ao.getTransactionCategory() == OperationCategoryEnum.DEBIT) {
                    if (ao.getAmountWithoutTax().compareTo(BigDecimal.ZERO) > 0) {
                        return "-1";
                    }
                    return "1";
                }
                if ( ao.getAmountWithoutTax().compareTo(BigDecimal.ZERO) > 0 ) {
                    return "1";
                }
                return "-1";
            }

            if(ao.getTransactionCategory() == OperationCategoryEnum.DEBIT) {
                if(ao.getTaxAmount() == null) return null;
                if (ao.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                    return "-1";
                }
                return "1";
            }
            if ( ao.getTaxAmount().compareTo(BigDecimal.ZERO) > 0 ) {
                return "1";
            }
            return "-1";         }

        protected String getBillingAcountCodeDescription(String code) {
            AccountingCode ac = accountingCodeService.findByCode(code);
            if(ac == null) return null;
            return ac.getDescription();
        }
        public CTLine initCTLine(AccountOperation ao, Invoice invoice, List<Map<String, Object>> ct_exploitations, CTLine ctLine) {

            ctLine.setRtype("C");
            ctLine.setRste("00050");
            String codeExp = getCodeExploitationANAEL(ct_exploitations);
            if (codeExp == null) {
                addError(ao.getId(),"Retab");
                return null;
            }
            ctLine.setRetab(codeExp);
            ctLine.setRex(this.getYYofCurrentYear()+"0");
            ctLine.setRperio("0");
            ctLine.setRperig("0");
            ctLine.setRperia("0");
            ctLine.setRce("6");
            ctLine.setDex(this.getYYofCurrentYear()+"0");
            if(invoice.getInvoiceDate() == null){
                addError(ao.getId(),"Ddatoa");
                return null;
            }
            ctLine.setDdatoa(getDegitsFromDate(invoice.getInvoiceDate(),"y"));
            ctLine.setDdatom(getDegitsFromDate(invoice.getInvoiceDate(),"m"));
            ctLine.setDdatoj(getDegitsFromDate(invoice.getInvoiceDate(),"d"));
            if(getCodeJournal(ct_exploitations) == null){
                addError(ao.getId(),"Djal");
                return null;
            }
            ctLine.setDjal(getCodeJournal(ct_exploitations));
             ctLine.setDmdev("0");
             ctLine.setDvala("0");
            ctLine.setDvalm("0");
            ctLine.setDvalj("0");
             ctLine.setDbord("0");
            ctLine.setDcontr("0");
            ctLine.setDmat1("0");
             ctLine.setDmat1b("0");
             ctLine.setDtx("0");
            ctLine.setDlet("0");
             ctLine.setDws("OCI");
            // to check if cf_num_piece null raise error or not
            if(ao.getSeller() == null ||ao.getSeller().getCfValue(CF_NUMÉRO_PIÉCE_COMPTABLE) == null){
                addError(ao.getId(),"Dint");
                return null;
            }
            ctLine.setDint(ao.getSeller().getCfValue(CF_NUMÉRO_PIÉCE_COMPTABLE).toString());
            ctLine.setDchron(ctLine.getDws()+ctLine.getDint());
            ctLine.setDident("OCI");
            ctLine.setDlbqe("0");
            ctLine.setDatgsa("0");
            ctLine.setDatgsm("0");
            ctLine.setDatgsj("0");
            ctLine.setDnbrel("0");
            ctLine.setDtrela("0");
            ctLine.setDtrelm("0");
            ctLine.setDtrelj("0");
            ctLine.setDthlet("0");
            ctLine.setDatlet("0");

            ctLine.setDsvech("0");
            ctLine.setDnbech("0");
             ctLine.setDsvper("0");

             ctLine.setAqte("0");
            ctLine.setAstd("0");
             ctLine.setAmat1("0");
             ctLine.setAthlet("0");
            ctLine.setAatlet("0");
            ctLine.setAlet("0");
             ctLine.setDbqe("0");
            ctLine.setDgui("0");
             ctLine.setDrib("0");
            ctLine.setDrelva("0");
            ctLine.setDrelvm("0");
            ctLine.setDrelvj("0");
            ctLine.setDnrel("0");
            ctLine.setDrbqa("0");
            ctLine.setDrbqm("0");
            ctLine.setDrbqj("0");
            ctLine.setDnban("0");
             ctLine.setRdebmj("0");
            ctLine.setRfinmj("0");

             ctLine.setRnat2("0");
             ctLine.setRnat3("0");
             ctLine.setRnat4("0");
             ctLine.setRnat5("0");
             ctLine.setRnat6("0");
             ctLine.setRnat7("0");
             ctLine.setRnat8("0");
             ctLine.setRnat9("0");
            ctLine.setLigana("0");

            return ctLine;
        }
        public Map<String, Object> prepareCTLine() {
            Map<String, Object> values = new HashMap<>();
            values.put(RTYPE_NAME, this.getRtype());
            values.put(RSTE_NAME, this.getRste());
            values.put(RETAB_NAME, this.getRetab());
            values.put(RGEN_NAME, this.getRgen());
            values.put(RAUX_NAME, this.getRaux());
            values.put(REX_NAME, this.getRex());
            values.put(RPERIO_NAME, this.getRperio());
            values.put(RPERIG_NAME, this.getRperig());
            values.put(RPERIA_NAME, this.getRperia());
            values.put(RANP_NAME, this.getRanp());
            values.put(RMOISP_NAME, this.getRmoisp());
            values.put(RJOURP_NAME, this.getRjourp());
            values.put(RCE_NAME, this.getRce());
            values.put(DEX_NAME, this.getDex());
            values.put(DDATOA_NAME, this.getDdatoa());
            values.put(DDATOM_NAME, this.getDdatom());
            values.put(DDATOJ_NAME, this.getDdatoj());
            values.put(dchron_name, this.getDchron());

            values.put(DJAL_NAME, this.getDjal());
            values.put(DPIECE_NAME, this.getDpiece());
            values.put(DTYPPI_NAME, this.getDtyppi());
            values.put(DLIB_NAME, this.getDlib());
            values.put(DMONT_NAME, this.getDmont());
            values.put(DCOEF_NAME, this.getDcoef());
            values.put(DMDEV_NAME, this.getDmdev());
            values.put(DECHA_NAME, this.getDecha());

            values.put(DECHM_NAME, this.getDechm());
            values.put(DECHJ_NAME, this.getDechj());
            values.put(DTYPRM_NAME, this.getDtyprm());
            values.put(DVALA_NAME, this.getDvala());
            values.put(DVALM_NAME, this.getDvalm());
            values.put(DVALJ_NAME, this.getDvalj());
            values.put(DSTA_1_NAME, this.getDsta1());
            values.put(DSTA_2_NAME, this.getDsta2());
            values.put(DTVA_NAME, this.getDtva());
            values.put(DBORD_NAME, this.getDbord());
            values.put(DCONTR_NAME, this.getDcontr());
            values.put(DMAT_1_NAME, this.getDmat1());
            values.put(DMAT_2_NAME, this.getDmat2());
            values.put(DMAT_1_B_NAME, this.getDmat1b());
            values.put(DMAT_2_B_NAME, this.getDmat2b());
            values.put(DVT_NAME, this.getDvt());
            values.put(DOB_NAME, this.getDob());
            values.put(DTX_NAME, this.getDtx());
            values.put(DLET_NAME, this.getDlet());
            values.put(DLIT_NAME, this.getDlit());
            values.put(DFOLIO_NAME, this.getDfolio());
            values.put(DWS_NAME, this.getDws());
            values.put(DINT_NAME, this.getDint());
            values.put(DNOLIG_NAME, this.getDnolig());
            values.put(DREGLT_NAME, this.getDreglt());
            values.put(DAFFEC_NAME, this.getDaffec());
            values.put(DAFFE_2_NAME, this.getDaffe2());
            values.put(DAFFE_3_NAME, this.getDaffe3());
            values.put(DIDENT_NAME, this.getDident());
            values.put(DCDEV_NAME, this.getDcdev());
            values.put(DLBQE_NAME, this.getDlbqe());
            values.put(DSTA_15_NAME, this.getDsta15());
            values.put(DATGSA_NAME, this.getDatgsa());
            values.put(DATGSM_NAME, this.getDatgsm());
            values.put(DATGSJ_NAME, this.getDatgsj());
            values.put(DREL_NAME, this.getDrel());
            values.put(DNBREL_NAME, this.getDnbrel());
            values.put(DTRELA_NAME, this.getDtrela());
            values.put(DTRELM_NAME, this.getDtrelm());
            values.put(DTRELJ_NAME, this.getDtrelj());
            values.put(DTHLET_NAME, this.getDthlet());
            values.put(DATLET_NAME, this.getDatlet());
            values.put(DSVECH_NAME, this.getDsvech());
            values.put(DNBECH_NAME, this.getDnbech());
            values.put(DSVREG_NAME, this.getDsvreg());
            values.put(DSVPER_NAME, this.getDsvper());
            values.put(DSGEN_NAME, this.getDsgen());
            values.put(DT_01_NAME, this.getDt01());
            values.put(DT_02_NAME, this.getDt02());
            values.put(DT_03_NAME, this.getDt03());
            values.put(DT_04_NAME, this.getDt04());
            values.put(DT_05_NAME, this.getDt05());
            values.put(DT_06_NAME, this.getDt06());
            values.put(DT_07_NAME, this.getDt07());
            values.put(DT_08_NAME, this.getDt08());
            values.put(DT_09_NAME, this.getDt09());
            values.put(DT_10_NAME, this.getDt10());
            values.put(DSTA_20_NAME, this.getDsta20());
            values.put(RSECT_NAME, this.getRsect());
            values.put(RCHAP_NAME, this.getRchap());
            values.put(RNAT_NAME, this.getRnat());
            values.put(RSYMB_NAME, this.getRsymb());
            values.put(RART_NAME, this.getRart());
            values.put(AQTE_NAME, this.getAqte());
            values.put(ASTD_NAME, this.getAstd());
            values.put(AUNCOT_NAME, this.getAuncot());
            values.put(AMAT_1_NAME, this.getAmat1());
            values.put(AMAT_2_NAME, this.getAmat2());
            values.put(ATHLET_NAME, this.getAthlet());
            values.put(AATLET_NAME, this.getAatlet());
            values.put(ALET_NAME, this.getAlet());
            values.put(AT_01_NAME, this.getAt01());
            values.put(AT_02_NAME, this.getAt02());
            values.put(AT_03_NAME, this.getAt03());
            values.put(AT_04_NAME, this.getAt04());
            values.put(AT_05_NAME, this.getAt05());
            values.put(AT_06_NAME, this.getAt06());
            values.put(AT_07_NAME, this.getAt07());
            values.put(AT_08_NAME, this.getAt08());
            values.put(AT_09_NAME, this.getAt09());
            values.put(AT_10_NAME, this.getAt10());
            values.put(DLIB_1_NAME, this.getDlib1());
            values.put(DLIB_2_NAME, this.getDlib2());
            values.put(DLIB_3_NAME, this.getDlib3());
            values.put(DLIB_4_NAME, this.getDlib4());
            values.put(DBQE_NAME, this.getDbqe());
            values.put(DGUI_NAME, this.getDgui());
            values.put(DCPTE_NAME, this.getDcpte());
            values.put(DRIB_NAME, this.getDrib());
            values.put(DRELVA_NAME, this.getDrelva());
            values.put(DRELVM_NAME, this.getDrelvm());
            values.put(DRELVJ_NAME, this.getDrelvj());
            values.put(DNREL_NAME, this.getDnrel());
            values.put(DRBQA_NAME, this.getDrbqa());
            values.put(DRBQM_NAME, this.getDrbqm());
            values.put(DRBQJ_NAME, this.getDrbqj());
            values.put(DNBAN_NAME, this.getDnban());
            values.put(DENTRE_NAME, this.getDentre());
            values.put(DREFT_NAME, this.getDreft());
            values.put(DCODE_NAME, this.getDcode());
            values.put(DRCE_NAME, this.getDrce());
            values.put(DER_NAME, this.getDer());
            values.put(DERPTF_NAME, this.getDerptf());
            values.put(RDEBMJ_NAME, this.getRdebmj());
            values.put(RFINMJ_NAME, this.getRfinmj());
            values.put(RSECT_2_NAME, this.getRsect2());
            values.put(RCHAP_2_NAME, this.getRchap2());
            values.put(RNAT_2_NAME, this.getRnat2());
            values.put(RSECT_3_NAME, this.getRsect3());
            values.put(RCHAP_3_NAME, this.getRchap3());
            values.put(RNAT_3_NAME, this.getRnat3());
            values.put(RSECT_4_NAME, this.getRsect4());
            values.put(RCHAP_4_NAME, this.getRchap4());
            values.put(RNAT_4_NAME, this.getRnat4());
            values.put(RSECT_5_NAME, this.getRsect5());
            values.put(RCHAP_5_NAME, this.getRchap5());
            values.put(RNAT_5_NAME, this.getRnat5());
            values.put(RSECT_6_NAME, this.getRsect6());
            values.put(RCHAP_6_NAME, this.getRchap6());
            values.put(RNAT_6_NAME, this.getRnat6());
            values.put(RSECT_7_NAME, this.getRsect7());
            values.put(RCHAP_7_NAME, this.getRchap7());
            values.put(RNAT_7_NAME, this.getRnat7());
            values.put(RSECT_8_NAME, this.getRsect8());
            values.put(RCHAP_8_NAME, this.getRchap8());
            values.put(RNAT_8_NAME, this.getRnat8());
            values.put(RSECT_9_NAME, this.getRsect9());
            values.put(RCHAP_9_NAME, this.getRchap9());
            values.put(RNAT_9_NAME, this.getRnat9());
            values.put(LIGANA_NAME, this.getLigana());
            values.put(ASTA_36_NAME, this.getAsta36());
            values.put(DERS_NAME, this.getDers());
            return values;
        }
        public void saveCTlines(List<Map<String, Object>> valuesList) {
            customTableService.create(TAB_FAN350P1, TAB_FAN350P1, valuesList);
        }
        String checkString(String object,int length){
            if (object == null) return object;
            if (object.length() > length )
                return object.substring(0,length);
            return object;
        }
        public String getRtype() {
            return checkString(rtype,1);
        }

        public void setRtype(String rtype) {
            this.rtype = rtype;
        }

        public String getRste() {
            return checkString(rste,5);
        }

        public void setRste(String rste) {
            this.rste = rste;
        }

        public String getRetab() {
            return checkString(retab,2);
        }

        public void setRetab(String retab) {
            this.retab = retab;
        }
        public String getRgenCode(){
            return rgen;
        }
        public String getRgen() {
            return checkString(rgen,6);
        }

        public void setRgen(String rgen) {
            this.rgen = rgen;
        }

        public String getRaux() {
            return checkString(raux,8);
        }

        public void setRaux(String raux) {
            this.raux = raux;
        }

        public String getRex() {
            return checkString(rex,3);
        }

        public void setRex(String rex) {
            this.rex = rex;
        }

        public String getRperio() {
            return checkString(rperio,2);
        }

        public void setRperio(String rperio) {
            this.rperio = rperio;
        }

        public String getRperig() {
            return checkString(rperig,2);
        }

        public void setRperig(String rperig) {
            this.rperig = rperig;
        }

        public String getRperia() {
            return checkString(rperia,2);
        }

        public void setRperia(String rperia) {
            this.rperia = rperia;
        }

        public String getRanp() {
            return checkString(ranp,2);
        }

        public void setRanp(String ranp) {
            this.ranp = ranp;
        }

        public String getRmoisp() {
            return checkString(rmoisp,2);
        }

        public void setRmoisp(String rmoisp) {
            this.rmoisp = rmoisp;
        }

        public String getRjourp() {
            return checkString(rjourp,2);
        }

        public void setRjourp(String rjourp) {
            this.rjourp = rjourp;
        }

        public String getRce() {
            return checkString(rce,1);
        }

        public void setRce(String rce) {
            this.rce = rce;
        }

        public String getDex() {
            return checkString(dex,3);
        }

        public void setDex(String dex) {
            this.dex = dex;
        }

        public String getDdatoa() {
            return checkString(ddatoa,2);
        }

        public void setDdatoa(String ddatoa) {
            this.ddatoa = ddatoa;
        }

        public String getDdatom() {
            return checkString(ddatom,2);
        }

        public void setDdatom(String ddatom) {
            this.ddatom = ddatom;
        }

        public String getDdatoj() {
            return checkString(ddatoj,2);

        }

        public void setDdatoj(String ddatoj) {
            this.ddatoj = ddatoj;
        }

        public String getDchron() {
            return checkString(dchron,7);
        }

        public void setDchron(String dchron) {
            this.dchron = dchron;
        }

        public String getDjal() {
            return checkString(djal,3);
        }

        public void setDjal(String djal) {
            this.djal = djal;
        }

        public String getDpiece() {
            if(dpiece != null)
                if (dpiece.length() > 8) return dpiece.substring(1, 9);
            return dpiece;
        }

        public void setDpiece(String dpiece) {
            this.dpiece = dpiece;
        }

        public String getDtyppi() {
            return checkString(dtyppi,2);
        }

        public void setDtyppi(String dtyppi) {
            this.dtyppi = dtyppi;
        }

        public String getDlib() {
            return checkString(dlib,25);
        }

        public void setDlib(String dlib) {
            this.dlib = dlib;
        }

        public String getDmont() {
            return checkString(dmont,15);
        }

        public void setDmont(String dmont) {
            this.dmont = dmont;
        }

        public String getDcoef() {
            return checkString(dcoef,8);
        }

        public void setDcoef(String dcoef) {
            this.dcoef = dcoef;
        }

        public String getDmdev() {
            return checkString(dmdev,18);
        }

        public void setDmdev(String dmdev) {
            this.dmdev = dmdev;
        }

        public String getDecha() {
            return checkString(decha,4);
        }

        public void setDecha(String decha) {
            this.decha = decha;
        }

        public String getDechm() {
            return checkString(dechm,2);
        }

        public void setDechm(String dechm) {
            this.dechm = dechm;
        }

        public String getDechj() {
            return checkString(dechj,2);
        }

        public void setDechj(String dechj) {
            this.dechj = dechj;
        }

        public String getDtyprm() {
            return checkString(dtyprm,2);
        }

        public void setDtyprm(String dtyprm) {
            this.dtyprm = dtyprm;
        }

        public String getDvala() {
            return checkString(dvala,2);
        }

        public void setDvala(String dvala) {
            this.dvala = dvala;
        }

        public String getDvalm() {
            return checkString(dvalm,2);
        }

        public void setDvalm(String dvalm) {
            this.dvalm = dvalm;
        }

        public String getDvalj() {
            return checkString(dvalj,2);
        }

        public void setDvalj(String dvalj) {
            this.dvalj = dvalj;
        }

        public String getDsta1() {
            return checkString(dsta1,2);
        }

        public void setDsta1(String dsta1) {
            this.dsta1 = dsta1;
        }

        public String getDsta2() {
            return checkString(dsta2,3);
        }

        public void setDsta2(String dsta2) {
            this.dsta2 = dsta2;
        }

        public String getDtva() {
            return checkString(dtva,1);
        }

        public void setDtva(String dtva) {
            this.dtva = dtva;
        }

        public String getDbord() {
            return checkString(dbord,8);
        }

        public void setDbord(String dbord) {
            this.dbord = dbord;
        }

        public String getDcontr() {
            return checkString(dbord,6);
        }

        public void setDcontr(String dcontr) {
            this.dcontr = dcontr;
        }

        public String getDmat1() {
            return checkString(dmat1,8);
        }

        public void setDmat1(String dmat1) {
            this.dmat1 = dmat1;
        }

        public String getDmat2() {
            return checkString(dbord,8);
        }

        public void setDmat2(String dmat2) {
            this.dmat2 = dmat2;
        }

        public String getDmat1b() {
            return checkString(dmat1b,6);
        }

        public void setDmat1b(String dmat1b) {
            this.dmat1b = dmat1b;
        }

        public String getDmat2b() {
            return checkString(dmat1b,8);
        }

        public void setDmat2b(String dmat2b) {
            this.dmat2b = dmat2b;
        }

        public String getDvt() {
            return checkString(dvt,2);
        }

        public void setDvt(String dvt) {
            this.dvt = dvt;
        }

        public String getDob() {
            return checkString(dob,2);
        }

        public void setDob(String dob) {
            this.dob = dob;
        }

        public String getDtx() {
            return checkString(dtx,13);
        }

        public void setDtx(String dtx) {
            this.dtx = dtx;
        }

        public String getDlet() {
            return checkString(dlet,3);
        }

        public void setDlet(String dlet) {
            this.dlet = dlet;
        }

        public String getDlit() {
            return checkString(dlit,2);
        }

        public void setDlit(String dlit) {
            this.dlit = dlit;
        }

        public String getDfolio() {
            return checkString(dfolio,3);
        }

        public void setDfolio(String dfolio) {
            this.dfolio = dfolio;
        }

        public String getDws() {
            return checkString(dws,3);
        }

        public void setDws(String dws) {
            this.dws = dws;
        }

        public String getDint() {
            return checkString(dint,12);
        }

        public void setDint(String dint) {
            this.dint = dint;
        }

        public String getDnolig() {
            return checkString(dnolig,5);
        }

        public void setDnolig(String dnolig) {
            this.dnolig = dnolig;
        }

        public String getDreglt() {
            return checkString(dreglt,2);
        }

        public void setDreglt(String dreglt) {
            this.dreglt = dreglt;
        }

        public String getDaffec() {
            return checkString(daffec,8);
        }

        public void setDaffec(String daffec) {
            this.daffec = daffec;
        }

        public String getDaffe2() {
            return checkString(daffe2,8);
        }

        public void setDaffe2(String daffe2) {
            this.daffe2 = daffe2;
        }

        public String getDaffe3() {
            return checkString(daffe3,8);
        }

        public void setDaffe3(String daffe3) {
            this.daffe3 = daffe3;
        }

        public String getDident() {
            return checkString(dident,3);
        }

        public void setDident(String dident) {
            this.dident = dident;
        }

        public String getDcdev() {
            return checkString(dcdev,3);
        }

        public void setDcdev(String dcdev) {
            this.dcdev = dcdev;
        }

        public String getDlbqe() {
            return checkString(dlbqe,3);
        }

        public void setDlbqe(String dlbqe) {
            this.dlbqe = dlbqe;
        }

        public String getDsta15() {
            return checkString(dsta15,15);
        }

        public void setDsta15(String dsta15) {
            this.dsta15 = dsta15;
        }

        public String getDatgsa() {
            return checkString(datgsa,2);
        }

        public void setDatgsa(String datgsa) {
            this.datgsa = datgsa;
        }

        public String getDatgsm() {
            return checkString(datgsm,2);
        }

        public void setDatgsm(String datgsm) {
            this.datgsm = datgsm;
        }

        public String getDatgsj() {
            return checkString(datgsj,2);
        }

        public void setDatgsj(String datgsj) {
            this.datgsj = datgsj;
        }

        public String getDrel() {
            return checkString(drel,1);
        }

        public void setDrel(String drel) {
            this.drel = drel;
        }

        public String getDnbrel() {
            return checkString(dnbrel,3);
        }

        public void setDnbrel(String dnbrel) {
            this.dnbrel = dnbrel;
        }

        public String getDtrela() {
            return checkString(dtrela,2);
        }

        public void setDtrela(String dtrela) {
            this.dtrela = dtrela;
        }

        public String getDtrelm() {
            return checkString(dtrelm,2);
        }

        public void setDtrelm(String dtrelm) {
            this.dtrelm = dtrelm;
        }

        public String getDtrelj() {
            return checkString(dtrelj,2);
        }

        public void setDtrelj(String dtrelj) {
            this.dtrelj = dtrelj;
        }

        public String getDthlet() {
            return checkString(dthlet,6);
        }

        public void setDthlet(String dthlet) {
            this.dthlet = dthlet;
        }

        public String getDatlet() {
            return checkString(datlet,6);
        }

        public void setDatlet(String datlet) {
            this.datlet = datlet;
        }

        public String getDsvech() {
            return checkString(dsvech,8);
        }

        public void setDsvech(String dsvech) {
            this.dsvech = dsvech;
        }

        public String getDnbech() {
            return checkString(dnbech,3);
        }

        public void setDnbech(String dnbech) {
            this.dnbech = dnbech;
        }

        public String getDsvreg() {
            return checkString(dsvreg,2);
        }

        public void setDsvreg(String dsvreg) {
            this.dsvreg = dsvreg;
        }

        public String getDsvper() {
            return checkString(dsvper,2);
        }

        public void setDsvper(String dsvper) {
            this.dsvper = dsvper;
        }

        public String getDsgen() {
            return checkString(dsgen,6);
        }

        public void setDsgen(String dsgen) {
            this.dsgen = dsgen;
        }

        public String getDt01() {
            return checkString(dt01,1);
        }

        public void setDt01(String dt01) {
            this.dt01 = dt01;
        }

        public String getDt02() {
            return checkString(dt02,1);
        }

        public void setDt02(String dt02) {
            this.dt02 = dt02;
        }

        public String getDt03() {
            return checkString(dt03,1);
        }

        public void setDt03(String dt03) {
            this.dt03 = dt03;
        }

        public String getDt04() {
            return checkString(dt04,1);
        }

        public void setDt04(String dt04) {
            this.dt04 = dt04;
        }

        public String getDt05() {
            return checkString(dt05,1);
        }

        public void setDt05(String dt05) {
            this.dt05 = dt05;
        }

        public String getDt06() {
            return checkString(dt06,1);
        }

        public void setDt06(String dt06) {
            this.dt06 = dt06;
        }

        public String getDt07() {
            return checkString(dt07,1);
        }

        public void setDt07(String dt07) {
            this.dt07 = dt07;
        }

        public String getDt08() {
            return checkString(dt08,1);
        }

        public void setDt08(String dt08) {
            this.dt08 = dt08;
        }

        public String getDt09() {
            return checkString(dt09,1);
        }

        public void setDt09(String dt09) {
            this.dt09 = dt09;
        }

        public String getDt10() {
            return checkString(dt10,1);
        }

        public void setDt10(String dt10) {
            this.dt10 = dt10;
        }

        public String getDsta20() {
            return checkString(dsta20,20);
        }

        public void setDsta20(String dsta20) {
            this.dsta20 = dsta20;
        }

        public String getRsect() {
            return checkString(rsect,6);
        }

        public void setRsect(String rsect) {
            this.rsect = rsect;
        }

        public String getRchap() {
            return checkString(rchap,2);
        }

        public void setRchap(String rchap) {
            this.rchap = rchap;
        }

        public String getRnat() {
            return checkString(rnat,6);
        }

        public void setRnat(String rnat) {
            this.rnat = rnat;
        }

        public String getRsymb() {
            return checkString(rsymb,6);
        }

        public void setRsymb(String rsymb) {
            this.rsymb = rsymb;
        }

        public String getRart() {
            return checkString(rart,15);
        }

        public void setRart(String rart) {
            this.rart = rart;
        }

        public String getAqte() {
            return checkString(aqte,21);
        }

        public void setAqte(String aqte) {
            this.aqte = aqte;
        }

        public String getAstd() {
            return checkString(astd,21);
        }

        public void setAstd(String astd) {
            this.astd = astd;
        }

        public String getAuncot() {
            return checkString(auncot,2);
        }

        public void setAuncot(String auncot) {
            this.auncot = auncot;
        }

        public String getAmat1() {
            return checkString(amat1,6);
        }

        public void setAmat1(String amat1) {
            this.amat1 = amat1;
        }

        public String getAmat2() {
            return checkString(amat2,8);
        }

        public void setAmat2(String amat2) {
            this.amat2 = amat2;
        }

        public String getAthlet() {
            return checkString(athlet,6);
        }

        public void setAthlet(String athlet) {
            this.athlet = athlet;
        }

        public String getAatlet() {
            return checkString(aatlet,6);
        }

        public void setAatlet(String aatlet) {
            this.aatlet = aatlet;
        }

        public String getAlet() {
            return checkString(alet,5);
        }

        public void setAlet(String alet) {
            this.alet = alet;
        }

        public String getAt01() {
            return checkString(at01,1);
        }

        public void setAt01(String at01) {
            this.at01 = at01;
        }

        public String getAt02() {
            return checkString(at02,1);
        }

        public void setAt02(String at02) {
            this.at02 = at02;
        }

        public String getAt03() {
            return checkString(at03,1);
        }

        public void setAt03(String at03) {
            this.at03 = at03;
        }

        public String getAt04() {
            return checkString(at04,1);
        }

        public void setAt04(String at04) {
            this.at04 = at04;
        }

        public String getAt05() {
            return checkString(at05,1);
        }

        public void setAt05(String at05) {
            this.at05 = at05;
        }

        public String getAt06() {
            return checkString(at06,1);
        }

        public void setAt06(String at06) {
            this.at06 = at06;
        }

        public String getAt07() {
            return checkString(at07,1);
        }

        public void setAt07(String at07) {
            this.at07 = at07;
        }

        public String getAt08() {
            return checkString(at08,1);
        }

        public void setAt08(String at08) {
            this.at08 = at08;
        }

        public String getAt09() {
            return checkString(at09,1);
        }

        public void setAt09(String at09) {
            this.at09 = at09;
        }

        public String getAt10() {
            return checkString(at10,1);
        }

        public void setAt10(String at10) {
            this.at10 = at10;
        }

        public String getDlib1() {
            return checkString(dlib1,25);
        }

        public void setDlib1(String dlib1) {
            this.dlib1 = dlib1;
        }

        public String getDlib2() {
            return checkString(dlib2,25);
        }

        public void setDlib2(String dlib2) {
            this.dlib2 = dlib2;
        }

        public String getDlib3() {
            return checkString(dlib3,25);
        }

        public void setDlib3(String dlib3) {
            this.dlib3 = dlib3;
        }

        public String getDlib4() {
            return checkString(dlib4,25);
        }

        public void setDlib4(String dlib4) {
            this.dlib4 = dlib4;
        }

        public String getDbqe() {
            return checkString(dbqe,5);
        }

        public void setDbqe(String dbqe) {
            this.dbqe = dbqe;
        }

        public String getDgui() {
            return checkString(dgui,5);
        }

        public void setDgui(String dgui) {
            this.dgui = dgui;
        }

        public String getDcpte() {
            return checkString(dcpte,11);
        }

        public void setDcpte(String dcpte) {
            this.dcpte = dcpte;
        }

        public String getDrib() {
            return checkString(drib,2);
        }

        public void setDrib(String drib) {
            this.drib = drib;
        }

        public String getDrelva() {
            return checkString(drelva,2);
        }

        public void setDrelva(String drelva) {
            this.drelva = drelva;
        }

        public String getDrelvm() {
            return checkString(drelvm,2);
        }

        public void setDrelvm(String drelvm) {
            this.drelvm = drelvm;
        }

        public String getDrelvj() {
            return checkString(drelvj,2);
        }

        public void setDrelvj(String drelvj) {
            this.drelvj = drelvj;
        }

        public String getDnrel() {
            return checkString(dnrel,3);
        }

        public void setDnrel(String dnrel) {
            this.dnrel = dnrel;
        }

        public String getDrbqa() {
            return checkString(drbqa,2);
        }

        public void setDrbqa(String drbqa) {
            this.drbqa = drbqa;
        }

        public String getDrbqm() {
            return checkString(drbqm,2);
        }

        public void setDrbqm(String drbqm) {
            this.drbqm = drbqm;
        }

        public String getDrbqj() {
            return checkString(drbqj,2);
        }

        public void setDrbqj(String drbqj) {
            this.drbqj = drbqj;
        }

        public String getDnban() {
            return checkString(dnban,2);
        }

        public void setDnban(String dnban) {
            this.dnban = dnban;
        }

        public String getDentre() {
            return checkString(dentre,1);
        }

        public void setDentre(String dentre) {
            this.dentre = dentre;
        }

        public String getDreft() {
            return checkString(dreft,10);
        }

        public void setDreft(String dreft) {
            this.dreft = dreft;
        }

        public String getDcode() {
            return checkString(dcode,1);
        }

        public void setDcode(String dcode) {
            this.dcode = dcode;
        }

        public String getDrce() {
            return checkString(drce,1);
        }

        public void setDrce(String drce) {
            this.drce = drce;
        }

        public String getDer() {
            return checkString(der,200);
        }

        public void setDer(String der) {
            this.der = der;
        }

        public String getDerptf() {
            return checkString(derptf,25);
        }

        public void setDerptf(String derptf) {
            this.derptf = derptf;
        }

        public String getRdebmj() {
            return checkString(rdebmj,12);
        }

        public void setRdebmj(String rdebmj) {
            this.rdebmj = rdebmj;
        }

        public String getRfinmj() {
            return checkString(rfinmj,12);
        }

        public void setRfinmj(String rfinmj) {
            this.rfinmj = rfinmj;
        }

        public String getRsect2() {
            return checkString(rsect2,6);
        }

        public void setRsect2(String rsect2) {
            this.rsect2 = rsect2;
        }

        public String getRchap2() {
            return checkString(rchap2,2);
        }

        public void setRchap2(String rchap2) {
            this.rchap2 = rchap2;
        }

        public String getRnat2() {
            return checkString(rnat2,6);
        }

        public void setRnat2(String rnat2) {
            this.rnat2 = rnat2;
        }

        public String getRsect3() {
            return checkString(rsect3,6);
        }

        public void setRsect3(String rsect3) {
            this.rsect3 = rsect3;
        }

        public String getRchap3() {
            return checkString(rchap3,2);
        }

        public void setRchap3(String rchap3) {
            this.rchap3 = rchap3;
        }

        public String getRnat3() {
            return checkString(rnat3,6);
        }

        public void setRnat3(String rnat3) {
            this.rnat3 = rnat3;
        }

        public String getRsect4() {
            return checkString(rsect4,6);
        }

        public void setRsect4(String rsect4) {
            this.rsect4 = rsect4;
        }

        public String getRchap4() {
            return checkString(rchap4,2);
        }

        public void setRchap4(String rchap4) {
            this.rchap4 = rchap4;
        }

        public String getRnat4() {
            return checkString(rnat4,6);
        }

        public void setRnat4(String rnat4) {
            this.rnat4 = rnat4;
        }

        public String getRsect5() {
            return checkString(rsect5,6);
        }

        public void setRsect5(String rsect5) {
            this.rsect5 = rsect5;
        }

        public String getRchap5() {
            return checkString(rchap5,2);
        }

        public void setRchap5(String rchap5) {
            this.rchap5 = rchap5;
        }

        public String getRnat5() {
            return checkString(rnat5,6);
        }

        public void setRnat5(String rnat5) {
            this.rnat5 = rnat5;
        }

        public String getRsect6() {
            return checkString(rsect6,6);
        }

        public void setRsect6(String rsect6) {
            this.rsect6 = rsect6;
        }

        public String getRchap6() {
            return checkString(rchap6,2);
        }

        public void setRchap6(String rchap6) {
            this.rchap6 = rchap6;
        }

        public String getRnat6() {
            return checkString(rnat6,6);
        }

        public void setRnat6(String rnat6) {
            this.rnat6 = rnat6;
        }

        public String getRsect7() {
            return checkString(rsect7,6);
        }

        public void setRsect7(String rsect7) {
            this.rsect7 = rsect7;
        }

        public String getRchap7() {
            return checkString(rchap7,2);
        }

        public void setRchap7(String rchap7) {
            this.rchap7 = rchap7;
        }

        public String getRnat7() {
            return checkString(rnat7,6);
        }

        public void setRnat7(String rnat7) {
            this.rnat7 = rnat7;
        }

        public String getRsect8() {
            return checkString(rsect8,6);
        }

        public void setRsect8(String rsect8) {
            this.rsect8 = rsect8;
        }

        public String getRchap8() {
            return checkString(rchap8,2);
        }

        public void setRchap8(String rchap8) {
            this.rchap8 = rchap8;
        }

        public String getRnat8() {
            return checkString(rnat8,6);
        }

        public void setRnat8(String rnat8) {
            this.rnat8 = rnat8;
        }

        public String getRsect9() {
            return checkString(rsect9,6);
        }

        public void setRsect9(String rsect9) {
            this.rsect9 = rsect9;
        }

        public String getRchap9() {
            return checkString(rchap9,2);
        }

        public void setRchap9(String rchap9) {
            this.rchap9 = rchap9;
        }

        public String getRnat9() {
            return checkString(rnat9,6);
        }

        public void setRnat9(String rnat9) {
            this.rnat9 = rnat9;
        }

        public String getLigana() {
            return checkString(ligana,5);
        }

        public void setLigana(String ligana) {
            this.ligana = ligana;
        }

        public String getAsta36() {
            return checkString(asta36,36);
        }

        public void setAsta36(String asta36) {
            this.asta36 = asta36;
        }

        public String getDers() {
            return checkString(ders,200);
        }

        public void setDers(String ders) {
            this.ders = ders;
        }
    }
    void addError(Long id,String msg) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put(id+"", " : " + msg);
        this.errorLog.add(errorDetails);
    }
    String generateError() {
        StringBuilder concatenatedErrors = new StringBuilder();
        int totalErrorDetails = errorLog.size();

        for (int i = 0; i < totalErrorDetails; i++) {
            Map<String, String> errorDetails = errorLog.get(i);

            for (Map.Entry<String, String> entry : errorDetails.entrySet()) {
                concatenatedErrors.append(entry.getKey()).append(entry.getValue());

                // Check if it's the last entry
                boolean isLastEntry = i == totalErrorDetails - 1 && entry.equals(errorDetails.entrySet().toArray()[errorDetails.size() - 1]);
                concatenatedErrors.append(isLastEntry ? "." : ",");
            }
        }

        return concatenatedErrors.toString();
    }
}
