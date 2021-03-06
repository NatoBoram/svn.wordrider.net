package cz.cvut.felk.timejuggler.swing.components.calendar;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JComponent;

import cz.cvut.felk.timejuggler.core.domain.DateInterval;
import cz.cvut.felk.timejuggler.entity.CalendarEvent;

/**
 * Tato trida zapouzdruje udalost v kalendari do graficke komponenty. Predstavuje vrchol hierarchie grafickych zobrazeni udalosti,
 * potomkove predstavuji specificke vykreslovani pro ruzne typy udalosti (tj. prepsat metodu paint(Graphics) ). O konstrukci instanci se stara {@link CalendarGridEventFactory}.
 * 
 * @author Jerry!
 */
public class CalendarGridEvent extends JComponent {

    private static final long serialVersionUID = 1L;

    private CalendarEvent calendarEvent = null;

    private DateInterval visibleDateInterval = null;
    
    protected CalendarGridEvent(CalendarEvent calendarEvent, DateInterval visibleDateInterval) {
		super();
		this.calendarEvent = calendarEvent;
		this.visibleDateInterval = visibleDateInterval;
		this.setToolTipText(calendarEvent.getName());
	}

	@Override
    public void paint(Graphics g) {
        //Basic. Zatim udelej obdelnicek
		Color color = calendarEvent.getColor();
		if (color==null) {
			color = Color.YELLOW; 
		}
        g.setColor(color);
        int arcWidth = 10;
        int arcHeight = 10;    
        g.fillRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, arcWidth, arcHeight);
        g.setColor(Color.BLACK);
        g.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, arcWidth, arcHeight);
        g.drawString(calendarEvent.getName(), 5, g.getFont().getSize());
        g.drawString(calendarEvent.getStartDate().toString(), 5, g.getFont().getSize() * 2);
        g.drawString(calendarEvent.getEndDate().toString(), 5, g.getFont().getSize() * 3);
        super.paint(g);
    }

    public CalendarEvent getCalendarEvent() {
        return calendarEvent;
    }

	public DateInterval getVisibleDateInterval() {
		return visibleDateInterval;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((calendarEvent == null) ? 0 : calendarEvent.hashCode());
		result = prime * result + ((visibleDateInterval == null) ? 0 : visibleDateInterval.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final CalendarGridEvent other = (CalendarGridEvent) obj;
		if (calendarEvent == null) {
			if (other.calendarEvent != null)
				return false;
		} else if (!calendarEvent.equals(other.calendarEvent))
			return false;
		if (visibleDateInterval == null) {
			if (other.visibleDateInterval != null)
				return false;
		} else if (!visibleDateInterval.equals(other.visibleDateInterval))
			return false;
		return true;
	}

}
