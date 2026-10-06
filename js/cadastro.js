if (CkgdAPI.isAutenticado()) window.location.replace("dashboard.html");

const cadastroForm = document.getElementById("form-cadastro");
const cadastroButton = document.getElementById("btn-finalizar-cadastro");
const cnpjInput = document.getElementById("input-cnpj");
const paisSelect = document.getElementById("input-pais");
const estadoSelect = document.getElementById("input-estado");
const estadoTexto = document.getElementById("input-estado-texto");
const senhaInput = document.getElementById("input-senha");
const confirmarSenhaInput = document.getElementById("input-confirmar-senha");

const ISO_COUNTRIES = "AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR BS BT BV BW BY BZ CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RE RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ TC TD TF TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW".split(" ");

const ESTADOS_BRASIL = [
  ["AC","Acre"],["AL","Alagoas"],["AP","Amapá"],["AM","Amazonas"],["BA","Bahia"],["CE","Ceará"],["DF","Distrito Federal"],["ES","Espírito Santo"],["GO","Goiás"],["MA","Maranhão"],["MT","Mato Grosso"],["MS","Mato Grosso do Sul"],["MG","Minas Gerais"],["PA","Pará"],["PB","Paraíba"],["PR","Paraná"],["PE","Pernambuco"],["PI","Piauí"],["RJ","Rio de Janeiro"],["RN","Rio Grande do Norte"],["RS","Rio Grande do Sul"],["RO","Rondônia"],["RR","Roraima"],["SC","Santa Catarina"],["SP","São Paulo"],["SE","Sergipe"],["TO","Tocantins"]
];

function preencherPaises() {
  let displayNames = null;
  try { displayNames = new Intl.DisplayNames(["pt-BR"], { type: "region" }); } catch (_) {}
  const countries = ISO_COUNTRIES.map(code => ({ code, nome: displayNames?.of(code) || code }))
    .sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"));
  countries.forEach(({ code, nome }) => {
    const option = document.createElement("option");
    option.value = code;
    option.textContent = nome;
    paisSelect.append(option);
  });
}

function configurarEstado() {
  const brasil = paisSelect.value === "BR";
  estadoSelect.replaceChildren();
  if (!paisSelect.value) {
    estadoSelect.append(new Option("Selecione primeiro o país", ""));
    estadoSelect.disabled = true;
    estadoSelect.hidden = false;
    estadoTexto.hidden = true;
    estadoTexto.disabled = true;
    estadoTexto.required = false;
    return;
  }
  if (brasil) {
    estadoSelect.append(new Option("Selecione o estado", ""));
    ESTADOS_BRASIL.forEach(([uf, nome]) => estadoSelect.append(new Option(`${nome} (${uf})`, uf)));
    estadoSelect.disabled = false;
    estadoSelect.hidden = false;
    estadoSelect.required = true;
    estadoTexto.hidden = true;
    estadoTexto.disabled = true;
    estadoTexto.required = false;
    estadoTexto.value = "";
  } else {
    estadoSelect.disabled = true;
    estadoSelect.hidden = true;
    estadoSelect.required = false;
    estadoTexto.hidden = false;
    estadoTexto.disabled = false;
    estadoTexto.required = true;
  }
}

function apenasDigitos(valor) { return String(valor || "").replace(/\D/g, "").slice(0, 14); }

function formatarCnpj(valor) {
  const d = apenasDigitos(valor);
  return d
    .replace(/^(\d{2})(\d)/, "$1.$2")
    .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1/$2")
    .replace(/(\d{4})(\d)/, "$1-$2");
}

function cnpjValido(valor) {
  const cnpj = apenasDigitos(valor);
  if (cnpj.length !== 14 || /^(\d)\1{13}$/.test(cnpj)) return false;
  const calcular = tamanho => {
    let soma = 0;
    let peso = tamanho - 7;
    for (let i = 0; i < tamanho; i++) {
      soma += Number(cnpj[i]) * peso--;
      if (peso < 2) peso = 9;
    }
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  };
  return calcular(12) === Number(cnpj[12]) && calcular(13) === Number(cnpj[13]);
}

function validarCnpjVisual() {
  const digits = apenasDigitos(cnpjInput.value);
  if (!digits.length) cnpjInput.setCustomValidity("Informe o CNPJ.");
  else if (digits.length !== 14) cnpjInput.setCustomValidity("O CNPJ deve ter exatamente 14 dígitos.");
  else if (!cnpjValido(digits)) cnpjInput.setCustomValidity("Informe um CNPJ válido.");
  else cnpjInput.setCustomValidity("");
}

cnpjInput.addEventListener("input", () => {
  cnpjInput.value = formatarCnpj(cnpjInput.value);
  validarCnpjVisual();
});
cnpjInput.addEventListener("blur", validarCnpjVisual);

paisSelect.addEventListener("change", configurarEstado);
preencherPaises();
configurarEstado();

cadastroForm.addEventListener("submit", async event => {
  event.preventDefault();
  if (cadastroButton.disabled) return;
  const error = document.getElementById("form-error");
  error.textContent = "";
  validarCnpjVisual();

  if (senhaInput.value !== confirmarSenhaInput.value) confirmarSenhaInput.setCustomValidity("As senhas não coincidem.");
  else confirmarSenhaInput.setCustomValidity("");

  if (!cadastroForm.checkValidity()) {
    cadastroForm.reportValidity();
    return;
  }

  const payload = {
    nomeEmpresa: document.getElementById("input-nome-empresa").value.trim(),
    cnpj: apenasDigitos(cnpjInput.value),
    email: document.getElementById("input-email").value.trim(),
    senha: senhaInput.value,
    pais: paisSelect.value,
    estado: paisSelect.value === "BR" ? estadoSelect.value : estadoTexto.value.trim(),
    cidade: document.getElementById("input-cidade").value.trim(),
    bairro: document.getElementById("input-bairro").value.trim(),
    endereco: document.getElementById("input-endereco").value.trim()
  };

  OasisUI.busy(cadastroButton, true, "Criando conta…");
  try {
    const auth = await CkgdAPI.cadastrar(payload);
    CkgdAPI.salvarSessao(auth);
    sessionStorage.setItem("oasis_welcome", "1");
    window.location.href = "dashboard.html";
  } catch (err) {
    error.textContent = err.message;
  } finally {
    OasisUI.busy(cadastroButton, false);
  }
});

confirmarSenhaInput.addEventListener("input", () => confirmarSenhaInput.setCustomValidity(""));
