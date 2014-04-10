package psi;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;


public class Msg implements Serializable{

	public enum Type {NONE, STAGE_ONE, DONE_STAGE_ONE, STAGE_TWO, DONE_STAGE_TWO}

	private static final long serialVersionUID = 1L;
	public Type type;
	public String origin;
	public ArrayList<String> operatedOnBy;
	public ArrayList<String> whoGot;
	public ArrayList<BigInteger[]> arrContent;
	public ArrayList<BigInteger> content;

	private Msg(){
		type = Type.NONE;
		origin = "";
		operatedOnBy = null;
		whoGot = null;
		arrContent = null;
		content = null;
	}

	private Msg(Type t,String or, ArrayList<String> by, ArrayList<BigInteger[]> arrCon, ArrayList<BigInteger> con ){
		type = t;
		origin = or;
		operatedOnBy = by;
		arrContent = arrCon;
		content = con;
		whoGot = new ArrayList<String>();
	}

	public static Msg createMyStageOneMsg(Intersect user){
		Type t_tmp = Type.STAGE_ONE;
		ArrayList<String> by_tmp = new ArrayList<String>();
		by_tmp.add(user.id);
		ArrayList<BigInteger[]> con_tmp = user.myData.encryptedFile;
		return new Msg(t_tmp,user.id, by_tmp, con_tmp, null );
	}

	public static Msg createMyStageTwoMsg(Intersect user){
		Type t_tmp = Type.STAGE_TWO;
		ArrayList<String> by_tmp = new ArrayList<String>();
		by_tmp.add(user.id);
		ArrayList<BigInteger> con_tmp = user.myData.encryptedIntersection;
		return new Msg(t_tmp,user.id, by_tmp, null, con_tmp );
	}

}
