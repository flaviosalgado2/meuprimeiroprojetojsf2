package br.com.cursojsf;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.bind.DatatypeConverter;

import com.google.gson.Gson;

import br.com.dao.DaoGeneric;
import br.com.entidades.Cidades;
import br.com.entidades.Estados;
import br.com.entidades.Pessoa;
import br.com.jpautil.JPAUtil;
import br.com.repository.IDaoPessoa;

@ViewScoped
@Named(value = "pessoaBean")
public class PessoaBean implements Serializable {

	private static final long serialVersionUID = -593274450187303447L;

	private Pessoa pessoa = new Pessoa();
	private List<Pessoa> pessoas = new ArrayList<Pessoa>();

	@Inject
	private DaoGeneric<Pessoa> daoGeneric;

	@Inject
	private IDaoPessoa iDaoPessoa;

	// teste CDI
	// @Inject
	// private EntityManager entityManager;

	private List<SelectItem> estados;

	private List<SelectItem> cidades;

	private Part arquivofoto;

	@Inject
	private JPAUtil jpaUtil;

	public List<SelectItem> getEstados() {
		estados = iDaoPessoa.listaEstados();
		return estados;
	}

	public void carregaCidades(AjaxBehaviorEvent event) {

		Estados estado = (Estados) ((HtmlSelectOneMenu) event.getSource()).getValue();

		if (estado != null) {
			pessoa.setEstados(estado);

			List<Cidades> cidades = jpaUtil.getEntityManager()
					.createQuery("from Cidades where estados.id = " + estado.getId()).getResultList();

			List<SelectItem> selectItemsCidade = new ArrayList<SelectItem>();

			for (Cidades cidade : cidades) {

				selectItemsCidade.add(new SelectItem(cidade, cidade.getNome()));
			}

			setCidades(selectItemsCidade);
		}
	}

	public void setCidades(List<SelectItem> cidades) {
		this.cidades = cidades;
	}

	public List<SelectItem> getCidades() {
		return cidades;
	}

	public Pessoa salvar() throws IOException {

		// processamento da imagem
		byte[] imagemByte = getByte(arquivofoto.getInputStream());

		// salvando imagem original
		pessoa.setFotoIconBase64Original(imagemByte);

		// transformando em buffer
		BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imagemByte));

		// pegando o tipo da imagem
		int type = bufferedImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : bufferedImage.getType();
		int largura = 200;
		int altura = 200;

		// criando miniatura da imagem
		BufferedImage resizeredImage = new BufferedImage(altura, altura, type);
		Graphics2D g = resizeredImage.createGraphics();
		g.drawImage(bufferedImage, 0, 0, largura, altura, null);
		g.dispose();

		// escrever novamente a imagem em tamanho menor
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String extensao = arquivofoto.getContentType().split("\\/")[1];
		ImageIO.write(resizeredImage, extensao, baos);

		String miniImagem = "data:" + arquivofoto.getContentType() + ";base64,"
				+ DatatypeConverter.printBase64Binary(baos.toByteArray());

		// processando imagem
		pessoa.setFotoIconBase64(miniImagem);
		pessoa.setExtensao(extensao);

		pessoa = daoGeneric.merge(pessoa);

		// pessoa = new Pessoa();

		carregarPessoas();

		mostrarMsg("Cadastrado com Sucesso!");

		return pessoa;
	}

	public void registraLog() {
		System.out.println("método registra log");
	}

	private void mostrarMsg(String msg) {

		FacesContext context = FacesContext.getCurrentInstance();
		FacesMessage message = new FacesMessage(msg);
		context.addMessage(null, message);

	}

	public Pessoa novo() {

		pessoa = new Pessoa();

		return pessoa;
	}

	public Pessoa limpar() {

		pessoa = new Pessoa();

		return pessoa;
	}

	public String remove() {

		daoGeneric.deletePorId(pessoa);

		pessoa = new Pessoa();

		carregarPessoas();
		mostrarMsg("Removido com Sucesso!");

		return "";
	}

	@PostConstruct
	public void carregarPessoas() {
		// manda para lista pessoas da classe
		pessoas = daoGeneric.getListEntity(Pessoa.class);
	}

	public Pessoa getPessoa() {
		return pessoa;
	}

	public void setPessoa(Pessoa pessoa) {
		this.pessoa = pessoa;
	}

	public DaoGeneric<Pessoa> getDaoGeneric() {
		return daoGeneric;
	}

	public void setDaoGeneric(DaoGeneric<Pessoa> daoGeneric) {
		this.daoGeneric = daoGeneric;
	}

	// sempre tem q ter o get para o JSF conseguir carregar na tela
	public List<Pessoa> getPessoas() {
		return pessoas;
	}

	public void pesquisaCep(AjaxBehaviorEvent event) {

		try {

			// isso e uma ajax feito em jsf, complica?
			URL url = new URL("https://viacep.com.br/ws/" + pessoa.getCep() + "/json/");

			URLConnection connection = url.openConnection();

			InputStream is = connection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			String cep = "";
			StringBuilder jsonCep = new StringBuilder();

			while ((cep = br.readLine()) != null) {
				jsonCep.append(cep);
			}

			Pessoa gsonAux = new Gson().fromJson(jsonCep.toString(), Pessoa.class);
			// pessoa.setCep(cep);

			// colocando
			pessoa.setCep(gsonAux.getCep());
			pessoa.setLogradouro(gsonAux.getLogradouro());
			pessoa.setComplemento(gsonAux.getComplemento());
			pessoa.setBairro(gsonAux.getBairro());
			pessoa.setLocalidade(gsonAux.getLocalidade());
			pessoa.setUf(gsonAux.getUf());
			pessoa.setUnidade(gsonAux.getUnidade());
			pessoa.setIbge(gsonAux.getIbge());
			pessoa.setGia(gsonAux.getGia());

		} catch (Exception e) {

			e.printStackTrace();
			mostrarMsg("Erro ao consultar o CEP");

		}
	}

	public String logar() {

		// System.out.println("teste de autenticacao : " + pessoa.getLogin() + "" +
		// pessoa.getSenha());

		Pessoa pessoaUser = iDaoPessoa.consultarUsuario(pessoa.getLogin(), pessoa.getSenha());

		if (pessoaUser != null) {// achou o usuario

			// add usuario na sessao usuarioLogado
			FacesContext context = FacesContext.getCurrentInstance();

			ExternalContext externalContext = context.getExternalContext();

			// externalContext.getSessionMap().put("usuarioLogado", pessoaUser.getLogin());
			externalContext.getSessionMap().put("usuarioLogado", pessoaUser);

			return "primeirapagina.jsf";
		}
		return "index.jsf";
	}

	public String deslogar() {

		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		externalContext.getSessionMap().remove("usuarioLogado");

		HttpServletRequest httpServletRequest = (HttpServletRequest) context.getCurrentInstance().getExternalContext()
				.getRequest();

		httpServletRequest.getSession().invalidate();

		return "index.jsf";
	}

	public boolean permiteAcesso(String acesso) {
		FacesContext context = FacesContext.getCurrentInstance();

		ExternalContext externalContext = context.getExternalContext();

		Pessoa pessoaUser = (Pessoa) externalContext.getSessionMap().get("usuarioLogado");

		return pessoaUser.getPerfilUser().equals(acesso);
	}

	@SuppressWarnings("unchecked")
	public String editar() {

		if (pessoa.getCidades() != null) {

			Estados estado = pessoa.getCidades().getEstados();
			pessoa.setEstados(estado);

			List<Cidades> cidades = jpaUtil.getEntityManager()
					.createQuery("from Cidades where estados.id = " + estado.getId()).getResultList();

			List<SelectItem> selectItemsCidade = new ArrayList<SelectItem>();

			for (Cidades cidade : cidades) {

				selectItemsCidade.add(new SelectItem(cidade, cidade.getNome()));
			}

			setCidades(selectItemsCidade);
		}

		return "";

	}

	public void setArquivofoto(Part arquivofoto) {
		this.arquivofoto = arquivofoto;
	}

	public Part getArquivofoto() {
		return arquivofoto;
	}

	// metodo que converte um input stream pra um array de bytes
	private byte[] getByte(InputStream is) throws IOException {
		int len;
		int size = 1024;
		byte[] buf = null;

		if (is instanceof ByteArrayInputStream) {

			size = is.available();
			buf = new byte[size];
			len = is.read(buf, 0, size);
		} else {

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			buf = new byte[size];

			while ((len = is.read(buf, 0, size)) != -1) {
				bos.write(buf, 0, len);
			}

			buf = bos.toByteArray();
		}
		return buf;
	}

	public void download() throws IOException {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		String fileDownloadId = params.get("fileDownloadId");

		Pessoa pessoa = daoGeneric.consultar(Pessoa.class, fileDownloadId);

		HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext()
				.getResponse();

		response.addHeader("Content-Disposition", "attachment; filename=download." + pessoa.getExtensao());
		response.setContentType("application/octet-stream");
		response.setContentLength(pessoa.getFotoIconBase64Original().length);
		response.getOutputStream().write(pessoa.getFotoIconBase64Original());
		response.getOutputStream().flush();

		FacesContext.getCurrentInstance().responseComplete();
	}
}
