import org.mindrot.jbcrypt.BCrypt;

public class JBCryptHasher {
	public static void main(String[] args) {
		System.out.println(BCrypt.hashpw(args[0], BCrypt.gensalt(12)));
	}
}