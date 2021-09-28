package fachada;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import dao.DAO;
import dao.DAOLog;
import dao.DAOMensagem;
import dao.DAOUsuario;
import modelo.Administrador;
import modelo.Log;
import modelo.Mensagem;
import modelo.Usuario;

public class Fachada {
	private static DAOUsuario daousuario = new DAOUsuario();  
	private static DAOMensagem daomensagem = new DAOMensagem();  
	private static DAOLog daolog = new DAOLog();  

	private static Usuario usuariologado=null;


	public static void inicializar() {
		DAO.open();
	}

	public static void finalizar(){
		DAO.close();
	}

	public static List<Usuario> listarUsuarios() {
		// nao precisa estar logado
		return daousuario.readAll();	
	}
	public static List<Mensagem> listarMensagens() {
		// nao precisa estar logado
		return daomensagem.readAll();	
	}

	public static List<Log> listarLogs() {
		// nao precisa estar logado
		return daolog.readAll();	
	}
	public static List<Mensagem> buscarMensagens(String termo) throws  Exception{
		/*
		 * nao precisa estar logado
		 * query no banco para obter mensagens do grupo que contenha
		 *  o termo (considerar case insensitive)
		 * 
		 */
		DAO.begin();
		List<Mensagem> retorno = DAOMensagem.queryMSGs(termo);
		if(retorno == null) {
			DAO.rollback();
			throw new Exception("não existe mensagem com este termo.");
		}
		DAO.commit();
		return retorno;
	}

	public static Usuario criarUsuario(String nome, String senha) throws  Exception{
		// nao precisa estar logado
		DAO.begin();	
		Usuario u = daousuario.read(nome+"/"+senha);
		if(u != null) {
			DAO.rollback();	
			throw new Exception("criar usuario - usuario existente:" + nome);
		}

		u = new Usuario(nome+"/"+senha);
		daousuario.create(u);		
		DAO.commit();
		return u;
	}


	public static void login(String nome, String senha) throws Exception{		
		//verificar se ja existe um usuario logada
		if(usuariologado!=null)
			throw new Exception ("ja existe um usuario logado"+getLogado());

		DAO.begin();	
		Usuario u = daousuario.read(nome+"/"+senha);	
		if(u == null) {
			DAO.rollback();	
			throw new Exception("login - usuario inexistente:" + nome);
		}
		if(!u.ativo()) {
			DAO.rollback();	
			throw new Exception("login - usuario nao ativo:" + nome);
		}
		usuariologado = u;		//altera o logado na fachada

		Log log = new Log(usuariologado.getNome() + " - login");
		daolog.create(log);
		DAO.commit();
	}
	public static void logoff() {		
		DAO.begin();
		Log log = new Log(usuariologado.getNome() + " - logoff");
		daolog.create(log);
		DAO.commit();

		usuariologado = null; 		//altera o logado na fachada
	}

	public static Usuario getLogado() {
		return usuariologado;
	}

	public static Mensagem criarMensagem(String texto) throws Exception{
		/*
		 * tem que esta logado
		 * criar a mensagem, onde o criador ï¿½ a usuario logada
		 * adicionar esta mensagem na lista de mensagens de cada usuario do grupo,
		 * incluindo a do criador
		 * retornar mensagem criada
		 */

		//para gerar o novo id da mensagem utilize:
//		int id = daomensagem.obterUltimoId();
//		id++;
//		Mensagem m = new Mensagem(id, usuariologado, texto);
		
		DAO.begin();
		Usuario usuariologado = getLogado();
		if(usuariologado == null) {
			DAO.rollback();
			throw new Exception("Usuario não está logado.");
		}
		Mensagem m = new Mensagem(usuariologado, texto);
		daomensagem.create(m);
		usuariologado.adicionar(m);
		DAO.commit();
		return m;
	}

	public static List<Mensagem> listarMensagensUsuario() throws Exception{
		/*
		 * tem que esta logado
		 * retorna todas as mensagens do usuario logado
		 * 
		 */
		if (getLogado() != null)
			return getLogado().getMensagens();
		else
			throw new Exception("O usuario não está logado");
	}

	public static void apagarMensagens(int... ids) throws  Exception{
		/*
		 * tem que esta logado
		 * recebe uma lista de numeros de id 
		 * (id ï¿½ um numero entre 1 a N, onde N ï¿½ a quatidade atual de mensagens do grupo)
		 * validar se ids sï¿½o de mensagens criadas pelo usuario logado
		 * (um usuario nao pode apagar mensagens de outros usuarios)
		 * 
		 * remover cada mensagem da lista de mensagens do usuario logado
		 * apagar cada mensagem do banco 
		 */
		DAO.begin();
		Usuario usuariologado = getLogado();
		if(usuariologado == null) {
			DAO.rollback();
			throw new Exception("Usuario não esta logado.");
		}
		for (int i : ids) {
			Mensagem m = daomensagem.read(i);
			if(usuariologado.getMensagens().stream().filter(item -> item.getId() == m.getId()).collect(Collectors.toList()).isEmpty()) {
				DAO.rollback();
				throw new Exception("Mensagem não pertence ao usuario logado.");
			}
			if(m == null) {
				DAO.rollback();
				throw new Exception("Mensagem não encontrada.");
			}
			daomensagem.delete(m);
			usuariologado.remover(m);
		}
		DAO.commit();
	}

	public static void sairDoGrupo() throws  Exception{
		/*
		 * tem que esta logado
		 * 
		 * criar a mensagem "fulano saiu do grupo"
		 * desativar o usuario logado e fazer logoff dele
		 */
		DAO.begin(); 
		Usuario usuarioLogado = getLogado();
		 if(usuarioLogado == null) {
			 DAO.rollback();
			 throw new Exception("Nenhum usuario logado.");
		 }
		 if(usuarioLogado.getNome().equals("admin")) {
			 DAO.rollback();
			 throw new Exception("Administrador não pode sair do grupo.");
		 }
		 String texto = usuarioLogado.getNome() + " saiu do grupo";
		 Mensagem m = new Mensagem(usuarioLogado, texto);
		 daomensagem.create(m);
		 usuarioLogado.desativar();
		 daousuario.update(usuarioLogado);
		 logoff();
		 DAO.commit();
	}

	//	public static int totalMensagensUsuario() throws Exception{
	//		/*
	//		 * tem que esta logado
	//		 * retorna total de mensagens criadas pelo usuario logado
	//		 * 
	//		 */
	//	}

	public static void esvaziar() throws Exception{
		DAO.clear();
	}

	/**************************************************************
	 * 
	 * NOVOS Mï¿½TODOS DA FACHADA PARA O PROJETO 2
	 * 
	 **************************************************************/

	public static Administrador criarAdministrador(String nome, String senha, String email) throws  Exception{
		// nao precisa estar logado
		DAO.begin();	
		Usuario u = daousuario.read(nome+"/"+senha);	
		if(u != null) {
			DAO.rollback();	
			throw new Exception("criar administrador - usuario ja existe:" + nome);
		}

		Administrador ad = new Administrador(nome+"/"+senha, email);
		daousuario.create(ad);		
		DAO.commit();
		return ad;
	}

	public static void solicitarAtivacao(String nome, String senha) throws  Exception{
		/*
		 * o usuario (nome+senha) tem que estar desativado
		 *  
		 * enviar um email para o administrador com a mensagem "nome solicita ativaï¿½ï¿½o"
		 * usar o mï¿½todo Fachada.enviarEmail(...) 
		 * 
		 */
		Usuario u = daousuario.read(nome+"/"+senha);
		if(u.ativo()) {
			throw new Exception("Usuário tem que estar desativado.");
		}
		enviarEmail("Solicitação de ativação", "Gostaria de solicitar minha ativação, ass. " + nome);
	}

	public static void solicitarExclusao(String nome, String senha) throws  Exception{
		/*
		 * o usuario (nome+senha) tem que estar desativado
		 *  
		 * enviar um email para o administrador com a mensagem "nome solicita exclusï¿½o"
		 * usar o mï¿½todo Fachada.enviarEmail(...) 
		 * 
		 */
		
		Usuario u = daousuario.read(nome+"/"+senha);
		if(u.ativo()) {
			throw new Exception("Usuário tem que estar desativado.");
		}
		enviarEmail("Solicitação de exclusão", "Gostaria de solicitar minha exclusão, ass. " + nome);
	}

	public static void ativarUsuario(String nome) throws  Exception{
		/*
		 * o usuario logado tem que ser um administrador  e o usuario a 
		 * ser ativado (nome) tem que estar desativado 
		 *  
		 * ativar o usuario 
		 * criar a mensagem "nome entrou no grupo"
		 * 
		 */
		DAO.begin();
		Usuario u = daousuario.GetUsuarioByNome(nome);
		if(!getLogado().getNome().equals("admin")) {
			DAO.rollback();
			throw new Exception("O usuario deve ser administrador para executar essa ação.");
		}
		if(u.ativo()) {
			DAO.rollback();
			throw new Exception("Usuario já está ativo.");
		}
		u.ativar();
		daousuario.update(u);
		criarMensagem(u.getNome() + " entrou no grupo");
		DAO.commit();
	}

	public static void apagarUsuario(String nome) throws  Exception{
		/*
		 * o usuario logado tem que ser um administrador  e o usuario a 
		 * ser apagado tem que estar desativado (e nï¿½o pode ser do tipo Administrador)
		 *  
		 * apagar as mensagens do usuario e apagar o usuario 
		 * criar a mensagem "nome foi excluido do sistema"
		 */
		DAO.begin();
		Usuario u = daousuario.GetUsuarioByNome(nome);
		if(!getLogado().getNome().equals("admin")) {
			DAO.rollback();
			throw new Exception("O usuario deve ser administrador para executar essa ação.");
		}
		if(u.ativo()) {
			DAO.rollback();
			throw new Exception("Usuario tem que estar desativado.");
		}
		
		daousuario.delete(u);
		criarMensagem(nome + " foi excluido do sistema");
		DAO.commit();
	}


	/**************************************************************
	 * 
	 * Mï¿½TODO PARA ENVIAR EMAIL, USANDO UMA CONTA (SMTP) DO GMAIL
	 * ELE ABRE UMA JANELA PARA PEDIR A SENHA DO EMAIL DO EMITENTE
	 * ELE USA A BIBLIOTECA JAVAMAIL (ver pom.xml)
	 * Lembrar de: 
	 * 1. desligar antivirus e de 
	 * 2. ativar opcao "Acesso a App menos seguro" na conta do gmail
	 * 
	 **************************************************************/
	public static void enviarEmail(String assunto, String mensagem) {
		try {
			/*
			 * ********************************************************
			 * Obs: lembrar de desligar antivirus e 
			 * de ativar "Acesso a App menos seguro" na conta do gmail
			 * 
			 * pom.xml contem a dependencia javax.mail
			 * 
			 * ********************************************************
			 */
			//configurar emails
			String emailorigem = "mashiroedu@gmail.com";
			String senhaorigem = pegarSenha();
			String emaildestino = "admimadim05@gmail.com";

			//Gmail
			Properties props = new Properties();
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.port", "587");
			props.put("mail.smtp.auth", "true");

			Session session;
			session = Session.getInstance(props,
					new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(emailorigem, senhaorigem);
				}
			});

			MimeMessage message = new MimeMessage(session);
			message.setSubject(assunto);		
			message.setFrom(new InternetAddress(emailorigem));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emaildestino));
			message.setText(mensagem);   // usar "\n" para quebrar linhas
			Transport.send(message);

			//System.out.println("enviado com sucesso");

		} catch (MessagingException e) {
			System.out.println(e.getMessage());
		}
	}
	
	/*
	 * JANELA PARA DIGITAR A SENHA DO EMAIL
	 */
	public static String pegarSenha(){
		JPasswordField field = new JPasswordField(10);
		field.setEchoChar('*'); 
		JPanel painel = new JPanel();
		painel.add(new JLabel("Entre com a senha do email:"));
		painel.add(field);
		JOptionPane.showMessageDialog(null, painel, "Senha", JOptionPane.PLAIN_MESSAGE);
		String texto = new String(field.getPassword());
		return texto.trim();
	}
}

