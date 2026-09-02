const ceps = ['15700001', '15700115', '15700234', '15700347', '15700458', '15700589'];
const adminId = 153;

const esportes = ['FUTEBOL', 'FUTSAL', 'VOLEI', 'BEACH_TENNIS', 'BASQUETE', 'TENIS'];

async function seed() {
  let count = 0;
  for (const cep of ceps) {
    if (count >= 5) break; // User asked for 5 quadras
    
    console.log(`Buscando CEP ${cep}...`);
    try {
      const brasilRes = await fetch(`https://brasilapi.com.br/api/cep/v2/${cep}`);
      const brasilData = await brasilRes.json();
      
      if (brasilData.erro) {
        console.log(`CEP ${cep} com erro.`);
        continue;
      }
      
      const query = encodeURIComponent(`${brasilData.street}, ${brasilData.city}, ${brasilData.state}`);
      const nomRes = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${query}`, {
        headers: { 'User-Agent': 'EquadrasApp/1.0' }
      });
      const nomText = await nomRes.text();
      let nomData;
      try {
        nomData = JSON.parse(nomText);
      } catch (err) {
        console.error('Error parsing Nominatim response:', nomText);
      }
      
      let lat = undefined;
      let lon = undefined;
      
      if (nomData && nomData.length > 0) {
        lat = parseFloat(nomData[0].lat);
        lon = parseFloat(nomData[0].lon);
      }
      
      const payload = {
        nome: `Quadra CEP ${cep.substring(0, 5)}-${cep.substring(5)}`,
        tipoEsporte: esportes[count % esportes.length],
        valorHora: 50.0 + (count * 10),
        cep: `${cep.substring(0, 5)}-${cep.substring(5)}`,
        logradouro: brasilData.street || '',
        bairro: brasilData.neighborhood || '',
        cidade: brasilData.city || '',
        estado: brasilData.state || '',
        latitude: lat,
        longitude: lon
      };

      console.log(`Salvando: ${payload.nome} | Lat/Lon: ${lat}/${lon}`);
      
      const apiRes = await fetch('http://localhost:8080/quadras', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Usuario-Id': adminId.toString()
        },
        body: JSON.stringify(payload)
      });
      
      if (apiRes.ok) {
        console.log(`Quadra salva com sucesso!`);
        count++;
      } else {
        const err = await apiRes.text();
        console.error(`Erro ao salvar: ${err}`);
      }
      
      // Delay to respect nominatim rate limit
      await new Promise(r => setTimeout(r, 1500));
      
    } catch (e) {
      console.error(e);
    }
  }
}

seed();
