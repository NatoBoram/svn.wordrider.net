package cz.cvut.felk.timejuggler.db;

/**
 * @author Jan Struz
 * @version 0.1
 * @created 12-V-2007 23:39:58
 * Hotovo
 */
public class RequestStatus extends DbElement{
	//TODO : Logging
	private String rstatus;
	
	private int componentId;

	public RequestStatus(){

	}
	
	public RequestStatus(String rstatus){
		this.rstatus = rstatus;
	}
	
    public void store() {

    }

	 /**
     * Method saveOrUpdate
     * @param template
     */
	public void saveOrUpdate(TimeJugglerJDBCTemplate template){
		if (getId() > 0) {
			Object params[] = { rstatus, componentId, getId() };
			String updateQuery = "UPDATE RequestStatus SET rstatus=?,calComponentID=? WHERE requestStatusID = ? ";
			template.executeUpdate(updateQuery, params);
		}else{
	        Object params[] = { rstatus, componentId };
	        String insertQuery = "INSERT INTO RequestStatus (rstatus,calComponentID) VALUES (?,?) ";
	        template.executeUpdate(insertQuery, params);
	        setId(template.getGeneratedId());
		}
	}
	
	 /**
     * Method delete
     * @param template
     */
	public void delete(TimeJugglerJDBCTemplate template){
		if (getId() > 0) {
			Object params[] = {	getId() };		
			String deleteQuery = "DELETE FROM RequestStatus WHERE requestStatusID = ?";
			template.executeUpdate(deleteQuery, params);
			setId(-1);
		}
	}


	public String getRstatus(){
		return rstatus;
	}

	/**
	 * 
	 * @param newVal
	 */
	public void setRstatus(String newVal){
		rstatus = newVal;
	}

	
	public void setComponentId(int componentId) {
		this.componentId = componentId; 
	}

	public int getComponentId() {
		return (this.componentId); 
	}

}