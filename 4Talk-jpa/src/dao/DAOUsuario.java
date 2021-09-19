package dao;

import javax.persistence.TypedQuery;

import modelo.Usuario;

public class DAOUsuario extends DAO<Usuario>  {

	@SuppressWarnings("unchecked")
	public Usuario read(Object chave) {
		String nomesenha = (String) chave;
		TypedQuery<Usuario> q = (TypedQuery<Usuario>) manager.createQuery("select u from Usuario u where u.nomesenha like :x");
		q.setParameter("x",nomesenha);
		Usuario usuario = q.getSingleResult();
		return usuario;
	}
	@SuppressWarnings("unchecked")
	public Usuario GetUsuarioByNome(String nome) {
		String nomee = (String) nome;
		TypedQuery<Usuario> q = (TypedQuery<Usuario>) manager.createQuery("select u from Usuario u where u.nomesenha like ~* :x");
		q.setParameter("x",nomee);
		Usuario usuario = q.getSingleResult();
		return usuario;
	}
}
