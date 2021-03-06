package cz.cvut.felk.timejuggler.db;

import cz.cvut.felk.timejuggler.db.entity.*;
import cz.cvut.felk.timejuggler.db.entity.interfaces.CategoryEntity;
import cz.cvut.felk.timejuggler.db.entity.interfaces.EventTaskEntity;
import cz.cvut.felk.timejuggler.db.entity.interfaces.VCalendarEntity;
import cz.cvut.felk.timejuggler.utilities.LogUtils;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.Calendars;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;


public class DbDataStore {
    private final static Logger logger = Logger.getLogger(DbDataStore.class.getName());

    //TODO : dopsat ICS export/import
    //TODO : upravit ukazkovy kod
    /**
     * Method main
     * @param args
     */
    public static void main(String[] args) {
        DbDataStore db = new DbDataStore();
        // Testing
        //importTest();
        db.showDB();
        System.exit(0);

        // Testing

        /* Vypis */

        List<VCalendarEntity> cals = db.getCalendars();

        int i = 0;
        for (VCalendarEntity cal : cals) {
            try {
                db.exportICS((VCalendar) cal, new File("vystup" + i + ".ics"));
                i++;
            } catch (URISyntaxException e) {
                LogUtils.processException(logger, e);
            } catch (ValidationException e) {
                LogUtils.processException(logger, e);
            } catch (IOException e) {
                LogUtils.processException(logger, e);
            }
        }

        db.showDB();
        try {
            ConnectionManager.getInstance().shutdown();
        }
        catch (SQLException e) {
            LogUtils.processException(logger, e);
        }
    }

    public DbDataStore() {

    }

    /**
     * Method importTest
     * @param Pro otestovani importu ICS
     */
    public static void importTest() {
        /*
           * Ukazkovy kod
           * ************************/
        DbDataStore db = new DbDataStore();
        /* pri importu je ulozeni do db automaticke */
        try {
            VCalendarEntity cal = db.importICS(new File("G:\\pokus\\USHolidays.ics"));
            /* nastaveni jmena pro importovany kalendar */
            cal.setName("USHolidays.ics - imported 1");
            /* ulozeni zmen */
            db.saveOrUpdate((VCalendar) cal);
            /* vypis obsahu db */
            db.showDB();

            /* vymazani kalendare z db */
            //db.delete(cal);
        }
        catch (IOException e) {
            LogUtils.processException(logger, e);
        }
        catch (ParserException e) {
            LogUtils.processException(logger, e);
        }
        catch (DatabaseException e) {
            LogUtils.processException(logger, e);
        }
        /* odpojeni databaze */
        /*try {
            ConnectionManager.getInstance().shutdown();
        }
        catch (SQLException ex) {
            LogUtils.processException(logger, ex);
        }*/
        // konec programu
    }

    /**
     * Method showDB
     * @param Pro vypis obsahu DB
     */
    public void showDB() {
        List<VCalendarEntity> cals = getCalendars();

        for (VCalendarEntity cal : cals) {
            System.out.println("Calendar name:" + cal.getName());
            System.out.println("+--Events:");

            List<EventTaskEntity> events = getEventsByCalendar((VCalendar) cal);
            if (events != null) {
                for (EventTaskEntity e : events) {
                    //System.out.println ("event.description: " + e.getDescription());
                    System.out.println("event.summary: " + e.getSummary());
                    System.out.println("+-startDate: " + e.getStartDate());
                    System.out.println("+-endDate: " + e.getEndDate());
                    System.out.println("+-created: " + e.getCreated());
                    System.out.println("+-dtstamp: " + e.getDTimestamp());

                    List<Category> cats = e.getCategories();
                    if (cats != null) {
                        System.out.print("+-categories ");
                        for (Object o : cats) {
                            CategoryEntity c = (CategoryEntity) o;
                            System.out.print(c.getName() + ",");
                        }
                        System.out.println();
                    }
                    Periods periods = e.getPeriods();
                    if (periods != null) {
                        for (cz.cvut.felk.timejuggler.db.entity.Period p : periods) {
                            //cz.cvut.felk.timejuggler.db.entity.Period p = (cz.cvut.felk.timejuggler.db.entity.Period) o;
                            //System.out.println("+-period " + p.getStartDate() + " ... " + p.getEndDate());
                            RepetitionRules rules = p.getRepetitionRules();
                            if (rules != null) {
                                for (RepetitionRule rr : rules) {
                                    System.out.println("+--REPEAT " + rr.getFrequency() +
                                            "; interval " + rr.getInterval() +
                                            "; byMonth " + rr.getByMonth() +
                                            "; byMonthDay " + rr.getByMonthDay());
                                }
                            }
                        }
                    }
                    //RepetitionRules rules;

                    System.out.println("++++++++++++++++++++++++++++++++++");
                }
            }
//            System.out.println("+--Todos:"); <<commented out by Vity >>
//            List<EventTask> todos = cal.getToDos();
//            if (todos != null) {
//                for (EventTask todo : todos) {
//                    System.out.println("todo: " + todo.getDescription());
//                }
//            }
            System.out.println();
        }
        if (cals.isEmpty()) {
            System.out.println("Zadne kalendare v databazi!");
        }
    }

    /**
     * Method getCalendars
     * @return
     */
    public List<VCalendarEntity> getCalendars() {
        String sql = "SELECT * FROM VCalendar";
        TimeJugglerJDBCTemplate<List<VCalendarEntity>> template = new TimeJugglerJDBCTemplate<List<VCalendarEntity>>() {
            protected void handleRow(ResultSet rs) throws SQLException {
                if (items == null) items = new ArrayList<VCalendarEntity>();
                VCalendar cal = new VCalendar();
                cal.setId(Integer.valueOf(rs.getInt("vCalendarID")).intValue());
                cal.setProductId(rs.getString("prodid"));
                cal.setCalendarScale(rs.getString("calscale"));
                cal.setMethod(rs.getString("method"));
                cal.setVersion(rs.getString("version"));
                cal.setName(rs.getString("name"));
                cal.setActive(rs.getInt("active") == 1 ? true : false);
                items.add(cal);
            }
        };
        try {
            template.executeQuery(sql, null);
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
        return template.getItems() == null ? new ArrayList<VCalendarEntity>() : template.getItems();
    }

    /**
     * Method getCategories
     * @return
     */
    public List<CategoryEntity> getCategories() {
        String sql = "SELECT DISTINCT * FROM Category";
        TimeJugglerJDBCTemplate<List<CategoryEntity>> template = new TimeJugglerJDBCTemplate<List<CategoryEntity>>() {
            protected void handleRow(ResultSet rs) throws SQLException {
                if (items == null) items = new ArrayList<CategoryEntity>();
                Category cat = new Category();
                cat.setId(rs.getInt("categoryID"));
                int col = rs.getInt("color");
                if (!rs.wasNull()) cat.setColor(new Color(col));
                cat.setName(rs.getString("name"));
                items.add(cat);
            }
        };
        try {
            template.executeQuery(sql, null);
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
        return template.getItems() == null ? new ArrayList<CategoryEntity>() : template.getItems();
    }

    /**
     * Method getEventsByCategory
     * @return Vraci vsechny udalosti podle zadane kategorie
     */

    public List<EventTaskEntity> getEventsByCategory() {
        //TODO getEventsByCategory
        return new ArrayList<EventTaskEntity>();
    }

    /**
     * Method getEventsByCalendar
     * @return Vraci vsechny udalosti typu Event v danem kalendari
     */
    public List<EventTaskEntity> getEventsByCalendar(VCalendar cal) {
        String sql = "SELECT * FROM VEvent,CalComponent,DateTime WHERE (VEvent.calComponentID = CalComponent.calComponentID AND CalComponent.vCalendarID=? AND CalComponent.dateTimeID=DateTime.dateTimeID)";
        Object params[] = {cal.getId()};
        TimeJugglerJDBCTemplate<List<EventTaskEntity>> template = new TimeJugglerJDBCTemplate<List<EventTaskEntity>>() {
            protected void handleRow(ResultSet rs) throws SQLException {
                if (items == null) items = new ArrayList<EventTaskEntity>();
                EventTask event = new EventTask();    // Vytvori udalost typu Event
                Timestamp ts;
                event.setId(rs.getInt("vEventID"));    //DB
                event.setLocation(rs.getString("location"));
                event.setTransparency(rs.getString("transp"));
                event.setPriority(rs.getInt("priority"));
                event.setGeoGPS(rs.getString("geo"));

                //cast calcomponent
                event.setComponentId(rs.getInt("calComponentID"));    //DB
                event.setDescription(rs.getString("description"));
                event.setUid(rs.getString("uid"));
                event.setClazz(rs.getString("clazz"));
                event.setOrganizer(rs.getString("organizer"));
                event.setSequence(rs.getInt("sequence"));
                event.setStatus(rs.getString("status"));
                event.setSummary(rs.getString("summary"));
                ts = rs.getTimestamp("dtstamp");
                if (ts != null) event.setDTimestamp(new Date(ts.getTime()));
                //event.setCalendar(cal);

                //cast DateTime
                event.getDateTime().setPeriodsId(rs.getInt("periodsID"));
                event.getDateTime().setDistinctDatesId(rs.getInt("distinctDatesID"));

                ts = rs.getTimestamp("lastmodified");
                if (ts != null) event.setLastModified(new Date(ts.getTime()));
                ts = rs.getTimestamp("created");
                if (ts != null) event.setCreated(new Date(ts.getTime()));
                ts = rs.getTimestamp("startDate");
                if (ts != null) event.setStartDate(new Date(ts.getTime()));
                ts = rs.getTimestamp("endDate");
                if (ts != null) event.setEndDate(new Date(ts.getTime()));

                // TODO: Nacitat Duration z DB (durationID)

                items.add(event);
            }
        };
        try {
            template.executeQuery(sql, params);
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
        return template.getItems() == null ? new ArrayList<EventTaskEntity>() : template.getItems();
    }

    /**
     * Method getToDosByCalendar
     * @return Vraci vsechny ukoly v danem kalendari
     */
    public List<EventTaskEntity> getToDosByCalendar(VCalendar cal) {
        String sql = "SELECT * FROM VToDo,CalComponent,DateTime WHERE (VToDo.calComponentID = CalComponent.calComponentID AND CalComponent.vCalendarID=? AND CalComponent.dateTimeID=DateTime.dateTimeID)";
        Object params[] = {cal.getId()};
        TimeJugglerJDBCTemplate<List<EventTaskEntity>> template = new TimeJugglerJDBCTemplate<List<EventTaskEntity>>() {
            protected void handleRow(ResultSet rs) throws SQLException {
                if (items == null) items = new ArrayList<EventTaskEntity>();
                EventTask todo = new EventTask(true);    // Vytvori udalost typu ToDo
                Timestamp ts;
                todo.setId(rs.getInt("vToDoID"));
                todo.setLocation(rs.getString("location"));
                todo.setPercentComplete(rs.getInt("percentcomplete"));
                todo.setPriority(rs.getInt("priority"));
                todo.setGeoGPS(rs.getString("geo"));
                ts = rs.getTimestamp("due");
                if (ts != null) todo.setEndDate(new Date(ts.getTime()));
                ts = rs.getTimestamp("completed");
                todo.setCompleted(new Date(ts.getTime()));
                //cast calcomponent
                todo.setComponentId(rs.getInt("calComponentID"));    //DB
                todo.setDescription(rs.getString("description"));
                todo.setUid(rs.getString("uid"));
                todo.setClazz(rs.getString("clazz"));
                todo.setOrganizer(rs.getString("organizer"));
                todo.setSequence(rs.getInt("sequence"));
                todo.setStatus(rs.getString("status"));
                todo.setSummary(rs.getString("summary"));
                ts = rs.getTimestamp("dtstamp");
                if (ts != null) todo.setDTimestamp(new Date(ts.getTime()));
                //todo.setCalendar(cal);

                //cast DateTime                
                todo.getDateTime().setPeriodsId(rs.getInt("periodsID"));
                todo.getDateTime().setDistinctDatesId(rs.getInt("distinctDatesID"));
                ts = rs.getTimestamp("lastmodified");
                if (ts != null) todo.setLastModified(new Date(ts.getTime()));
                ts = rs.getTimestamp("created");
                if (ts != null) todo.setCreated(new Date(ts.getTime()));
                ts = rs.getTimestamp("startDate");
                if (ts != null) todo.setStartDate(new Date(ts.getTime()));

                // TODO: Nacitat Duration z DB (durationID)

                items.add(todo);
            }
        };
        try {
            template.executeQuery(sql, params);
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
        return template.getItems() == null ? new ArrayList<EventTaskEntity>() : template.getItems();
    }

    /**
     * Method getAllEventsFromSelectedCalendars
     * @return
     */
    public List<EventTaskEntity> getEvents(Date startDate, Date endDate) {
        // TODO: Napsat SELECT pro vraceni udalosti mezi danymi casy
        return new ArrayList<EventTaskEntity>();
    }

    /**
     * Method saveOrUpdate
     * <p/>
     * Ulozi entitu do databaze
     */
    public <C extends DbElement> void saveOrUpdate(C entity) throws DatabaseException {
        TimeJugglerJDBCTemplate template = new TimeJugglerJDBCTemplate();
        entity.saveOrUpdate(template);
        template.commit();
    }

    /**
     * Method saveOrUpdate
     */
    public void saveOrUpdate(VCalendar cal) throws DatabaseException {
        TimeJugglerJDBCTemplate template = new TimeJugglerJDBCTemplate();
        cal.saveOrUpdate(template);
        template.commit();
    }

    /**
     * Method delete
     */
    public void delete(VCalendar cal) throws DatabaseException {
        TimeJugglerJDBCTemplate template = new TimeJugglerJDBCTemplate();
        List<EventTaskEntity> events = getEventsByCalendar(cal);

        for (EventTaskEntity event : events) {
            ((EventTask) event).delete(template);
        }

        /*
        List<EventTaskEntity> todos = getToDosByCalendar(cal);
        if (todos != null) {
            for (EventTaskEntity todo : todos) {
                ((EventTask)todo).delete(template);
            }
        }
        */
        cal.delete(template);
        template.commit();
    }

    /**
     * Method store
     */
    public <C extends CalComponent> void saveOrUpdate(VCalendar cal, C component) throws DatabaseException {
        // Pridani noveho Eventu nebo Ukolu do kalendare
        TimeJugglerJDBCTemplate template = new TimeJugglerJDBCTemplate();
        // Kalendar musi byt jiz v databazi
        component.setCalendar(cal);
        component.saveOrUpdate(template);
        template.commit();
    }

    /**
     * Method delete
     */
    public <C extends CalComponent> void delete(C component) throws DatabaseException {
        // Odstraneni Eventu nebo Ukolu z kalendare
        TimeJugglerJDBCTemplate template = new TimeJugglerJDBCTemplate();
        component.delete(template);
        template.commit();
    }

    /**
     * Method delete
     */
    //TODO nejak generalizovat pro vsechny potomky DBElementu pokud mozno
    public void delete(Category component) throws DatabaseException {
        // Odstraneni Eventu nebo Ukolu z kalendare
        TimeJugglerJDBCTemplate template = new TimeJugglerJDBCTemplate();
        component.delete(template);
        template.commit();
    }


    /**
     * Method importICS
     * @return
     */
    public VCalendarEntity importICS(File file) throws IOException, ParserException, DatabaseException {
        logger.info("Importing ICS file " + file);
        TimeJugglerJDBCTemplate template = new TimeJugglerJDBCTemplate();    // import jako 1 transakce

        // TODO: periods,..
        // Import je castecne funkcni
        Property prop;

        // Vytvoreni nove instance tridy VCalendar (typu Timejuggler)
        VCalendar newcal = new VCalendar();

        // Vytvoreni nove instance tridy Calendar (typu iCal) ze souboru ICS
        Calendar calendar = Calendars.load(file.getAbsolutePath());

        // Instance pomocne tridy pro prevod objektu iCal na typ Timejuggler
        ICalTransformer transformer = ICalTransformer.getInstance();

        // Nastaveni vlastnosti novemu kalendari, ktere byly precteny ze souboru
        prop = calendar.getMethod();
        newcal.setMethod(prop == null ? "" : prop.getValue());
        prop = calendar.getCalendarScale();
        newcal.setCalendarScale(prop == null ? "" : prop.getValue());
        prop = calendar.getProductId();
        newcal.setProductId(prop == null ? "" : prop.getValue());
        prop = calendar.getVersion();
        newcal.setVersion(prop == null ? "" : prop.getValue());
        newcal.setName(file.getAbsolutePath());

        // Ulozeni nastaveni kalendare do DB
        newcal.saveOrUpdate(template);

        // Pro kazdou komponentu VEVENT ze souboru, vytvor instanci VEvent a uloz ji do kalendare
        ComponentList complist = calendar.getComponents(Component.VEVENT);

        for (Object obj : complist) {
            Component comp = (Component) obj;
            // TODO: rozlisit Component.VEVENT a Component.VTODO

            EventTask event = new EventTask();

            prop = comp.getProperty(Property.CLASS);
            event.setClazz(prop == null ? "" : prop.getValue());
            prop = comp.getProperty(Property.CREATED);
            event.setCreated(prop == null ? null : new Date(((Created) prop).getDateTime().getTime()));
            prop = comp.getProperty(Property.DESCRIPTION);
            event.setDescription(prop == null ? "" : prop.getValue());
            prop = comp.getProperty(Property.DTSTART);
            event.setStartDate(prop == null ? null : (((DtStart) prop).getDate()));
            prop = comp.getProperty(Property.GEO);
            event.setGeoGPS(prop == null ? "" : ((prop).getValue()));
            prop = comp.getProperty(Property.LAST_MODIFIED);
            event.setLastModified(prop == null ? null : new Date(((LastModified) prop).getDateTime().getTime()));
            prop = comp.getProperty(Property.LOCATION);
            event.setLocation(prop == null ? "" : prop.getValue());
            prop = comp.getProperty(Property.ORGANIZER);
            event.setOrganizer(prop == null ? "" : prop.getValue());
            prop = comp.getProperty(Property.PRIORITY);
            event.setPriority(prop == null ? 0 : ((Priority) prop).getLevel());
            prop = comp.getProperty(Property.DTSTAMP);
            event.setDTimestamp(prop == null ? null : (new Date(((DtStamp) prop).getDateTime().getTime())));
            prop = comp.getProperty(Property.SEQUENCE);
            event.setSequence(prop == null ? 0 : ((Sequence) prop).getSequenceNo());
            prop = comp.getProperty(Property.STATUS);
            event.setStatus(prop == null ? "" : (prop).getValue());
            prop = comp.getProperty(Property.SUMMARY);
            event.setSummary(prop == null ? "" : (prop).getValue());
            prop = comp.getProperty(Property.TRANSP);
            event.setTransparency(prop == null ? "" : (prop).getValue());
            prop = comp.getProperty(Property.URL);
            event.setUrl(prop == null ? "" : prop.getValue());
            prop = comp.getProperty(Property.RECURRENCE_ID);
            event.setRecurrenceId(prop == null ? null : new Date(((RecurrenceId) prop).getDate().getTime()));
            prop = comp.getProperty(Property.DTEND);
            event.setEndDate(prop == null ? null : ((DtEnd) prop).getDate());
            prop = comp.getProperty(Property.DURATION);
            if (prop != null) event.setEndDate(transformer.makeDuration(prop));
            prop = comp.getProperty(Property.UID);
            event.setUid(prop == null ? "" : (prop).getValue());

            /* Categories */
            prop = comp.getProperty(Property.CATEGORIES);
            if (prop != null) {
                CategoryList catList = ((net.fortuna.ical4j.model.property.Categories) prop).getCategories();    // iCal
                Category cat;    // Timejuggler

                List<CategoryEntity> cats = getCategories();
                String catName;
                for (Iterator<?> it = catList.iterator(); it.hasNext();) {
                    catName = it.next().toString();
                    //TODO if catName exists,...else create new
                    cat = new Category(catName);
                    if (!cats.contains(cat)) {
                        cat.saveOrUpdate(template);
                        cats.add(cat);
                    }
                    event.addCategory(cat);    //TODO import: pridat Category z DB!, nebo vytvorit novou kategorii, pokud jiz existuje
                }
            }

            /* Cast Periods + Recurrence Dates */
            /* priprava */

            cz.cvut.felk.timejuggler.db.entity.interfaces.PeriodsEntity eventPeriods = event.getPeriods();

            prop = comp.getProperty(Property.RDATE);
            RDate rdate = (RDate) prop;

            PeriodList plist;
            Periods periods = new Periods();
            cz.cvut.felk.timejuggler.db.entity.Period newPeriod;

            if (rdate != null) {
                plist = rdate.getPeriods();
                //Periods
                if (plist != null) {
                    for (Object pobj : plist) {
                        net.fortuna.ical4j.model.Period p = (net.fortuna.ical4j.model.Period) pobj;
                        newPeriod = transformer.makePeriod(p);
                        periods.addPeriod(newPeriod);
                    }
                    //POZDEJI ! event.setPeriods(periods);
                }

                //Dates
                DateList dlist = rdate.getDates();
                DistinctDates ds = new DistinctDates();
                for (Object o : dlist) {
                    Date d = (Date) o;
                    ds.addDate(new DistinctDate(d));
                }
                event.setDistinctDates(ds);

            }
            //TODO : DbDataStore - Rules
            RRule rrule;
            /* poradi vyhodnocovani opakovani je v RFC 2445 [Page 44] */

            rrule = (RRule) comp.getProperty(Property.RRULE);
            if (rrule != null) {

                Recur recur = rrule.getRecur();    //iCal
                RepetitionRules rrs = new RepetitionRules();
                //for (Object o : ){
                RepetitionRule rr = new RepetitionRule();
                if (recur.getFrequency().equals(Recur.DAILY)) {
                    rr.setFrequency(RepetitionRule.DAILY);
                } else if (recur.getFrequency().equals(Recur.HOURLY)) {
                    rr.setFrequency(RepetitionRule.HOURLY);
                } else if (recur.getFrequency().equals(Recur.MINUTELY)) {
                    rr.setFrequency(RepetitionRule.MINUTELY);
                } else if (recur.getFrequency().equals(Recur.MONTHLY)) {
                    rr.setFrequency(RepetitionRule.MONTHLY);
                } else if (recur.getFrequency().equals(Recur.SECONDLY)) {
                    rr.setFrequency(RepetitionRule.SECONDLY);
                } else if (recur.getFrequency().equals(Recur.WEEKLY)) {
                    rr.setFrequency(RepetitionRule.WEEKLY);
                } else if (recur.getFrequency().equals(Recur.YEARLY)) {
                    rr.setFrequency(RepetitionRule.YEARLY);
                }
                assert recur.getHourList() != null;
                rr.setByHour(recur.getHourList().toString());
                rr.setByWeekNo(recur.getWeekNoList().toString());
                rr.setByYearDay(recur.getYearDayList().toString());
                rr.setBySetPosition(recur.getSetPosList().toString());
                rr.setByMonth(recur.getMonthList().toString());
                rr.setByMinute(recur.getMinuteList().toString());
                rr.setByMonthDay(recur.getMonthDayList().toString());
                //rr.set ... (recur.getSecondList()); vterinove opakovani.. nevedeme :)
                rr.setInterval(recur.getInterval());
                rr.setRepeat(recur.getCount());

                rrs.addRule(rr);
                //}

                newPeriod = new cz.cvut.felk.timejuggler.db.entity.Period();

                newPeriod.setRepetitionRules(rrs);
                newPeriod.setEndDate(recur.getUntil());

                periods.addPeriod(newPeriod);

            }

            //rrule = (RRule)comp.getProperty(Property.EXRULE);
            //rrule = (RRule)comp.getProperty(Property.EXDATE);

            event.setPeriods(periods);

            //eventPeriods.

            /* TODO: + ? , Alarms */

            //Ulozeni eventu do kalendare
            event.setCalendar(newcal);
            event.saveOrUpdate(template);
        }
        template.commit();    // potvrzeni transakce
        return newcal;
    }

    /**
     * Export ICS dat do souboru
     * @param calendar   ????
     * @param outputFile vystupni soubor pro ulozeni dat
     * @throws URISyntaxException  ????
     * @throws ValidationException Chyba - Nevalidni ical
     * @throws IOException         Chyba IO pri zapisu
     */
    public void exportICS(VCalendar calendar, File outputFile) throws URISyntaxException, ValidationException, IOException {
        logger.info("Exporting calendar to file " + outputFile.getPath() + "...");
        // TODO: Period property
        // Funkcni - castecne exportuje Eventy
        Calendar ical;
        List<EventTaskEntity> events = getEventsByCalendar(calendar);    // sada Timejuggler
        ComponentList compList = new ComponentList();    // sada pro iCal

        PropertyList propList;
        ComponentFactory iCalFactory = ComponentFactory.getInstance();

        for (EventTaskEntity e : events) {
            propList = new PropertyList();

            // Nastaveni vlastnosti pro Eventy k ulozeni do souboru
            String value;
            value = e.getClazz();
            if (value != "") propList.add(new Clazz(value));
            Date tmpdate = e.getCreated();
            if (tmpdate != null) propList.add(new Created(new DateTime(tmpdate.getTime())));
            value = e.getDescription();
            if (value != "") propList.add(new Description(value));
            if (e.getStartDate() != null)
                propList.add(new DtStart(new net.fortuna.ical4j.model.Date(e.getStartDate())));

            value = e.getGeoGPS();
            if (value != "") propList.add(new Geo(value));
            tmpdate = e.getLastModified();
            if (tmpdate != null) propList.add(new LastModified(new DateTime(tmpdate.getTime())));

            value = e.getLocation();
            if (value != "") propList.add(new Location(value));


            value = e.getOrganizer();
            if (value != "") propList.add(new Organizer(value));


            propList.add(new Priority(e.getPriority()));
            propList.add(new DtStamp());
            propList.add(new Sequence(e.getSequence()));
            value = e.getStatus();
            if (value != "") propList.add(new Status(value));
            value = e.getSummary();
            if (value != "") propList.add(new Summary(value));
            value = e.getTransparency();
            if (value != "") propList.add(new Transp(value));
            try {
                value = e.getUrl();
                if (value != "") propList.add(new Url(new URI(value)));
            }
            catch (URISyntaxException ex) {
                LogUtils.processException(logger, ex);
            }
            //propList.add(new RecurrenceId());
            if (e.getEndDate() != null) propList.add(new DtEnd(new net.fortuna.ical4j.model.Date(e.getEndDate())));

            //TODO: nastavit duration nebo dtend property
            //propList.add(new Duration());
            value = e.getUid();
            if (value != null) propList.add(new Uid(value));

            net.fortuna.ical4j.model.Component comp = iCalFactory.createComponent(Component.VEVENT, propList);

            compList.add(comp);

            Periods periods = e.getPeriods();
            if (periods != null) {
                for (cz.cvut.felk.timejuggler.db.entity.Period p : periods) {
                    System.out.println("+-period " + p.getStartDate() + " ... " + p.getEndDate());
                }
            }
        }
        propList = new PropertyList();

        // Nastaveni vlastnosti kalendare
        // Povinne: ProductID, Version
        // Nepovinne: CalendarScale, Method
        String value;
        value = calendar.getProductId();
        if (value != "") propList.add(new ProdId(value));
        value = calendar.getVersion();
        if (value != "") propList.add(Version.VERSION_2_0);
        value = calendar.getCalendarScale();
        if (value != "") propList.add(new CalScale(value));
        value = calendar.getMethod();
        if (value != "") propList.add(new Method(value));

        // Vytvoreni objektu Calendar (typ iCal) pro ulozeni do souboru
        ical = new Calendar(propList, compList);

        CalendarOutputter exporter = new CalendarOutputter(true);
        OutputStream outStream = null;
        try {
            outStream = new FileOutputStream(outputFile);
            exporter.output(ical, outStream);
            logger.info("Export finished.");
        }
//	    catch (IOException ex) {
//    		// Chyba IO pri zapisu
//	    	LogUtils.processException(logger, ex);
//	    }
//	    catch(ValidationException ex){
//	    	// Chyba - Nevalidni ical
//	    	LogUtils.processException(logger, ex);
//	    }
        finally {
            if (outStream != null)
                outStream.close();
        }
    }


}
