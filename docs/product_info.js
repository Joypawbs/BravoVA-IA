// Port 1:1 de data/ProductDescriptionProvider.kt y model/BravoModels.kt (Product.nutritionProfile)
// p = {sku, name, price, cat (category), unit, bravo (isBravo), art (artType), badge}
(function () {
  const has = (s, ...k) => k.some(x => s.includes(x));
  const before = (s, d) => { const i = s.indexOf(d); return i < 0 ? s : s.slice(0, i); };

  function buildBravoCopy(p, n, cat, unit, brand) {
    if (cat.includes('pescad') || has(n, 'salmón', 'salmon', 'dorado', 'chillo', 'mero', 'camar', 'langost', 'pulpo', 'calamar', 'almeja', 'mejill', 'bacalao', 'tilapia')) {
      const prep = n.includes('ahumad') ? 'ahumado lento en madera noble con textura sedosa y laminado fino'
        : has(n, 'camar', 'langost') ? 'limpieza rigurosa, sabor marino dulce y textura firme al punto'
        : n.includes('bacalao') ? 'curado tradicional de textura firme, ideal tras desalado progresivo'
        : n.includes('filete') ? 'corte limpio sin espinas de cocción rápida en sartén, horno o airfryer'
        : 'pieza fresca de pesquería seleccionada bajo estricto control de hielo escarchado';
      return [`${p.name} (${unit}) de ${brand}: ${prep}. Empacado en sucursal Bravo para preservar su aroma oceánico y frescura natural.`, 'Cadena fría 0°C–2°C', 'Conservación y uso: mantén entre 0°C y 2°C y consume dentro de las 48 horas; salpimienta con limón y ajo justo antes de sellar.'];
    }
    if (cat.includes('embutid') || p.art === 'salami' || has(n, 'salami', 'longaniza', 'chorizo', 'jamón', 'jamon', 'tocineta', 'salchicha', 'mortadela', 'pepperoni')) {
      const style = n.includes('longaniza') ? 'receta criolla con naranja agria, orégano y ajo fresco molido'
        : n.includes('salami') ? 'curado equilibrado de corte firme, clásico para fritura crujiente o guisos dominicanos'
        : has(n, 'jamón', 'jamon', 'pavo') ? 'rebanado uniforme bajo en exceso de humedad, perfecto para sándwiches y picaderas'
        : n.includes('tocineta') ? 'ahumado uniforme con proporción balanceada de magro y veta dorada'
        : 'embutido sellado al vacío con especias seleccionadas y control térmico continuo';
      return [`${p.name} en presentación de ${unit} (${brand}): ${style}. Textura consistente y frescura verificada en cámara frigorífica.`, 'Sellado higiénico 0°C–4°C', 'Conservación y uso: refrigera entre 0°C y 4°C; una vez abierto, envuelve herméticamente y consume en un máximo de 5 días.'];
    }
    if (cat.includes('carne') || has(n, 'angus', 'churrasco', 'picaña', 'picana', 'ribeye', 'filete de res', 'chuleta', 'cerdo', 'pollo', 'muslo', 'pechuga', 'costilla', 'molida', 'mondongo', 'patitas')) {
      const cut = has(n, 'angus', 'ribeye', 'churrasco', 'picaña') ? 'corte con marmoleo natural y maduración controlada para máxima jugosidad a la parrilla'
        : has(n, 'chuleta', 'costilla', 'cerdo') ? 'porcionado uniforme de carne tierna ideal para brasa, horno o caldero criollo'
        : has(n, 'pollo', 'pechuga', 'muslo', 'alita') ? 'ave fresca procesada el mismo día sin hormonas añadidas, lista para sazonar'
        : n.includes('molida') ? 'molienda fresca en frío con balance preciso entre carne magra y sabor'
        : 'limpieza esmerada en carnicería certificada Bravo para cocción lenta o tradicional';
      return [`${p.name} (${unit}) seleccionado por maestros carniceros (${brand}): ${cut}.`, 'Corte fresco del día', 'Conservación y uso: mantén refrigerado a 0°C–4°C (o congela a -18°C); atempera 15 minutos antes de llevar al fuego.'];
    }
    if (cat.includes('lácteo') || cat.includes('lacteo') || has(n, 'queso', 'leche', 'yogur', 'mantequilla', 'huevo', 'crema')) {
      const d = n.includes('queso') ? 'pasta uniforme, aroma lácteo auténtico y excelente fundido o corte en lonjas'
        : n.includes('yogur') ? 'fermentación suave con cultivos activos y textura cremosa sin grumos'
        : n.includes('huevo') ? 'huevos frescos de granja clasificados por tamaño y cáscara limpia resistente'
        : n.includes('mantequilla') ? 'batido tradicional de crema pura con punto exacto para untar o repostería'
        : 'lácteo pasteurizado de alta rotación diaria con aporte natural de calcio y proteína';
      return [`${p.name} (${unit}) de ${brand}: ${d}.`, 'Rotación diaria refrigerada', 'Conservación y uso: refrigera entre 2°C y 5°C (leches UHT en lugar fresco hasta abrir); mantén cerrado tras cada porción.'];
    }
    if (cat.includes('congelado') || cat.includes('helado')) {
      const f = has(n, 'helado', 'paleta', 'sorbete') ? 'textura cremosa sin cristales de hielo, elaborado con base láctea o fruta real'
        : 'ultracongelación rápida (IQF) que bloquea sabor, crujencia y nutrientes listos en minutos';
      return [`${p.name} (${unit}) de ${brand}: ${f}.`, 'Cadena -18°C continua', 'Conservación y uso: guarda directamente a -18°C; cocina sin descongelar en horno, sartén o freidora de aire según empaque.'];
    }
    if (cat.includes('pan') || cat.includes('galleta') || has(n, 'casabe', 'bizcocho', 'croissant', 'tostada')) {
      const b = n.includes('casabe') ? 'tostado crujiente de yuca artesanal dominicana, naturalmente libre de gluten'
        : n.includes('galleta') ? 'horneado dorado de textura crocante en empaque resellable contra humedad'
        : 'miga tierna y corteza equilibrada horneada con harinas seleccionadas';
      return [`${p.name} (${unit}) de ${brand}: ${b}, ideal para desayunos, meriendas y acompañamientos.`, 'Horneado fresco Bravo', 'Conservación y uso: almacena en lugar fresco y seco dentro de su bolsa cerrada; tuesta ligeramente para reavivar su aroma.'];
    }
    if (cat.includes('fruta') || cat.includes('vegetal') || has(n, 'plátano', 'platano', 'aguacate', 'mango', 'yuca', 'papa', 'tomate', 'cebolla', 'ajo', 'limón')) {
      return [`${p.name} (${unit}) de cosecha seleccionada (${brand}): clasificación manual pieza por pieza en su punto óptimo de firmeza, color y sabor tropical.`, 'Cosecha fresca local', 'Conservación y uso: lava con agua potable justo antes de consumir; mantén raíces/tubérculos en sitio ventilado y hojas verdes refrigeradas.'];
    }
    if (cat.includes('aceite') || cat.includes('vinagre')) {
      const o = n.includes('vinagre') ? 'acidez balanceada y fermentación limpia ideal para aderezos, escabeches y lavado culinario'
        : 'extracción cuidada y alto punto de humo para salteados, frituras ligeras y aderezos en frío';
      return [`${p.name} (${unit}) de ${brand}: ${o}.`, 'Pureza y rendimiento', 'Conservación y uso: mantén bien tapado lejos de la luz directa y fuentes de calor para preservar su perfil aromático.'];
    }
    if (cat.includes('agua') || cat.includes('refresco') || has(n, 'jugo', 'malta', 'energ')) {
      return [`${p.name} (${unit}) de ${brand}: bebida refrescante lista para servir bien fría, con sellado inviolable y burbujeo o pureza mineral verificada.`, 'Hidratación al instante', 'Conservación y uso: refrigera entre 4°C y 8°C antes de servir para disfrutar su punto óptimo de frescor.'];
    }
    if (cat.includes('bodega') || has(n, 'vino', 'cerveza', 'ron', 'cava', 'sangr', 'whisky')) {
      return [`${p.name} (${unit}) de Bodega Bravo (${brand}): guarda controlada con equilibrio aromático para maridajes, celebraciones y sobremesa. Venta exclusiva +18.`, 'Cava climatizada +18', 'Servicio sugerido: sirve blancos, espumosos y cervezas entre 4°C y 8°C; tintos reserva entre 14°C y 17°C.'];
    }
    if (has(cat, 'alimentación', 'alimentacion', 'nuevo') || has(n, 'arroz', 'habichuela', 'pasta', 'café', 'cafe', 'sazón', 'sazon', 'miel', 'coco', 'harina', 'azúcar', 'azucar')) {
      const pn = n.includes('arroz') ? 'grano selecto entero de cocción suelta y rendimiento constante en caldero'
        : has(n, 'habichuela', 'gandul') ? 'legumbre calibrada de piel tierna y ablandado uniforme para guisos tradicionales'
        : has(n, 'café', 'cafe') ? 'tueste aromático de altura con notas tostadas intensas y molienda pareja'
        : n.includes('miel') ? 'miel pura de abeja con aroma floral natural y viscosidad densa sin azúcares añadidos'
        : n.includes('coco') ? 'pulpa de coco tropical de consistencia rica para habichuelas con dulce, arroces o postres'
        : 'ingrediente esencial de despensa con trazabilidad de lote y sellado contra humedad';
      return [`${p.name} (${unit}) de ${brand}: ${pn}.`, 'Despensa certificada', 'Conservación y uso: guarda en alacena fresca y seca; tras abrir envases secos o especias, cierra herméticamente.'];
    }
    if (cat.includes('snack') || cat.includes('dulce') || has(n, 'chocolate', 'maní', 'mani', 'papas', 'platanito', 'chicharr', 'almendra')) {
      return [`${p.name} (${unit}) de ${brand}: bocadillo crocante o confite balanceado, sellado en atmósfera protectora para conservar su textura crujiente desde el primer bocado.`, 'Empaque crujiente sellado', 'Conservación y uso: mantén en lugar seco a temperatura ambiente (evita sol directo en chocolates y frutos secos).'];
    }
    if (cat.includes('cuidado personal') || has(n, 'pasta dental', 'enjuague', 'cepillo', 'champ', 'shampoo', 'desodorante', 'jabón', 'jabon', 'gotero', 'protector', 'crema')) {
      return [`${p.name} (${unit}) de ${brand}: fórmula dermatológicamente probada para higiene, protección y rutina diaria personal con textura de rápida absorción o limpieza suave.`, 'Cuidado diario probado', 'Modo de uso: aplica la cantidad recomendada sobre piel, cabello o zona bucal limpia; evita contacto directo con los ojos.'];
    }
    if (cat.includes('bebé') || cat.includes('bebe')) {
      return [`${p.name} (${unit}) de ${brand}: formulado especialmente con ingredientes suaves e hipoalergénicos pensando en el confort y la piel delicada de tu bebé.`, 'Suavidad hipoalergénica', 'Uso y cuidado: revisa siempre talla o etapa recomendada y conserva en espacio limpio y seco fuera del alcance infantil.'];
    }
    return [`${p.name} (${unit}) de ${brand}: desempeño concentrado para limpieza profunda, desinfección y orden del hogar con máximo rendimiento por aplicación.`, 'Alto rendimiento hogar', 'Uso seguro: sigue la dosificación sugerida en etiqueta y mantén envases cerrados en área ventilada fuera del alcance de niños.'];
  }

  function buildArcaCopy(p, n, cat, unit, brand) {
    if (cat.includes('seco') || p.art === 'pet_kibble') {
      const t = has(n, 'gato', 'kitten') ? 'felinos con taurina esencial y control urinario'
        : has(n, 'cachorro', 'puppy') ? 'cachorros con DHA, calcio y proteína de alta digestibilidad'
        : 'perros con prebióticos, omega 3 y 6 y croqueta de palatabilidad superior';
      return [`${p.name} (${unit}) — ${brand} en El Arca Petshop: nutrición completa para ${t}, formulada para energía diaria y pelaje brillante.`, 'Nutrición balanceada', 'Guía veterinaria: divide la ración diaria en 2 tomas según peso y mantén agua fresca disponible; haz transición gradual en 7 días.'];
    }
    if (has(cat, 'húmedo', 'humedo', 'snack') || p.art === 'pet_wet' || p.art === 'pet_snack') {
      return [`${p.name} (${unit}) — ${brand}: receta jugosa o premio funcional de alta aceptación para hidratación extra, adiestramiento positivo y refuerzo digestivo.`, 'Alta palatabilidad', 'Guía veterinaria: ofrece como complemento o premio (máx. 10% de calorías diarias) y refrigera la porción sobrante tras abrir.'];
    }
    if (has(cat, 'farmacia', 'salud') || p.art === 'pet_health') {
      return [`${p.name} (${unit}) — ${brand} en El Arca VetCare: soporte clínico y preventivo formulado bajo estándares veterinarios para protección antiparasitaria, dermatológica o articular.`, 'Respaldo VetCare', 'Guía veterinaria: respeta dosificación exacta por peso y especie indicada en caja o consulta a nuestro veterinario en Brav IA.'];
    }
    if (has(cat, 'ave', 'pez', 'exótic', 'exotic')) {
      return [`${p.name} (${unit}) — ${brand}: producto especializado para hábitat, nutrición y bienestar de aves ornamentales, peces tropicales, tortugas y pequeños mamíferos.`, 'Especialidad exóticos', 'Guía veterinaria: verifica dosis por volumen de agua o peso de tu mascota exótica y mantén el hábitat limpio y ventilado.'];
    }
    if (has(cat, 'arena', 'hogar')) {
      return [`${p.name} (${unit}) — ${brand}: control superior de olores, aglomeración rápida o absorción higiénica libre de polvo excesivo para mantener fresco el hogar.`, 'Control total de olor', 'Guía práctica: retira residuos diariamente y renueva la bandeja o paño según frecuencia de uso de tus mascotas.'];
    }
    if (cat.includes('juguete') || p.art === 'pet_toy') {
      return [`${p.name} (${unit}) — ${brand}: diseño ergonómico y materiales seguros libres de tóxicos para estimular el instinto natural, reducir estrés y cuidar la dentadura.`, 'Material Pet-Safe', 'Guía de uso: supervisa el juego activo y revisa periódicamente el estado del juguete acorde al nivel de mordida.'];
    }
    return [`${p.name} (${unit}) — ${brand} en El Arca Petshop: accesorio resistente y ergonómico pensado para paseos seguros, descanso confortable o aseo sin estrés.`, 'Ergonomía y duración', 'Guía práctica: ajusta correas o accesorios dejando espacio cómodo de dos dedos y limpia regularmente con agua tibia.'];
  }

  function describe(p) {
    const n = p.name.toLowerCase(), cat = p.cat.toLowerCase();
    const unit = p.unit.trim() ? p.unit : 'Unidad';
    const isArca = p.sku.startsWith('ARC-');
    const brand = isArca && p.bravo ? 'Selección El Arca' : p.bravo ? 'Marca Exclusiva Bravo'
      : p.badge.trim() ? p.badge : (isArca ? 'El Arca Petshop' : 'Marca Seleccionada');
    const [summary, spec, tip] = isArca ? buildArcaCopy(p, n, cat, unit, brand) : buildBravoCopy(p, n, cat, unit, brand);
    return { summary, highlights: [brand, `Presentación: ${unit}`, spec, isArca ? 'Garantía Veterinaria El Arca' : 'Calidad Certificada Bravo'], tip };
  }

  function nutrition(p) {
    const n = p.name.toLowerCase(), cat = p.cat.toLowerCase(), c = (...k) => has(n, ...k);
    const isNonFood = ['limpieza', 'cuidado', 'arena', 'accesorios'].includes(cat) ||
      ['soap', 'paper', 'shampoo', 'toothpaste', 'deodorant', 'litter', 'toy', 'collar', 'bed', 'bowl'].includes(p.art);
    if (isNonFood) {
      const comp = c('cloro') ? 'Hipoclorito de sodio al 5.25%, agua purificada e hidróxido de sodio estabilizante.'
        : c('detergente', 'jabón', 'lavaplatos') ? 'Tensioactivos aniónicos y no iónicos biodegradables, agentes ablandadores de agua, enzimas activas y fragancia.'
        : c('suavizante') ? ' Tensioactivos catiónicos acondicionadores de tejidos, microcápsulas de fragancia y conservantes.'
        : c('desinfectante') ? 'Cloruro de benzalconio, alcohol graso etoxilado, agua desmineralizada y esencia aromática.'
        : c('papel', 'servilleta', 'toalla') ? '100% fibras vírgenes de celulosa pura certificada de alta absorción.'
        : c('bolsa') ? 'Polietileno de alta densidad (HDPE) resistente a rasgaduras.'
        : c('shampoo', 'acondicionador') ? 'Agua purificada, lauril éter sulfato de sodio suave, pantenol (pro-vitamina B5), glicerina y extractos botánicos.'
        : c('crema dental', 'enjuague') ? 'Fluoruro de sodio (1450 ppm), sorbitol, sílice hidratada, xilitol y aroma de menta fresca.'
        : c('desodorante') ? 'Clorhidrato de aluminio, ciclometicona, alcohol estearílico, agentes humectantes y fragancia hipoalergénica.'
        : c('arena', 'tapete') ? 'Bentonita sódica natural ultra-aglomerante con carbón activado neutralizador de olores.'
        : 'Materiales hipoalergénicos certificados de alta durabilidad y grado doméstico seguro.';
      return { kcal: null, portion: 'Artículo no alimenticio (Sin aporte calórico)', prot: null, carbs: null, fat: null, ingredients: comp, allergens: ['Uso externo / No ingerible'], diets: ['No Comestible'] };
    }
    if (!p.bravo || ['perros', 'gatos', 'aves'].includes(cat)) {
      const kcal = c('húmedo', 'lata', 'pouch', 'paté') ? 98 : c('snack', 'galleta', 'hueso', 'premio') ? 325 : c('semilla', 'alpiste', 'heno') ? 290 : c('cachorro', 'kitten') ? 385 : 360;
      const ing = cat === 'gatos' ? 'Proteína seleccionada de salmón/pollo real, arroz cervecero, taurina esencial, ácidos grasos Omega 3 y 6, vitaminas A, D3, E y minerales quelados.'
        : cat === 'aves' ? 'Mezcla natural de mijo blanco, alpiste seleccionado, avena pelada, semillas de girasol y suplemento vitamínico fortificado.'
        : 'Carne real de pollo/res o cordero como primer ingrediente, arroz integral, pulpa de remolacha, glucosamina, Omega 3 y 6, vitaminas y antioxidantes.';
      return { kcal, portion: 'por cada 100 g (Nutrición Mascotas)', prot: '26.0 g', carbs: '38.0 g', fat: '14.0 g', ingredients: ing, allergens: ['Uso exclusivo para mascotas'], diets: ['Nutrición Animal Balanceada', 'Con Vitaminas y Taurina/Omega'] };
    }
    const T = [
      [() => c('aceite'), 884], [() => c('mantequilla de maní'), 588], [() => c('mantequilla'), 717], [() => c('margarina'), 540], [() => c('mayonesa'), 680],
      [() => c('arroz'), 360], [() => c('espagueti', 'pasta ', 'coditos', 'fideos', 'lasaña', 'macarrones'), 356], [() => c('avena'), 389], [() => c('harina', 'mezcla para pancake'), 364],
      [() => c('habichuela', 'lenteja', 'garbanzo', 'guandul'), 118], [() => c('maíz dulce'), 86], [() => c('azúcar'), 387], [() => c('miel'), 304], [() => c('sal ', 'sal marina', 'sal yodada'), 0],
      [() => c('vinagre'), 18], [() => c('salsa de tomate', 'pasta de tomate'), 82], [() => c('ketchup', 'catchup'), 112], [() => c('mostaza'), 66], [() => c('salsa soya'), 53],
      [() => c('sazón', 'caldo', 'sopita', 'orégano', 'ajo en pasta'), 145], [() => c('atún'), () => c('agua') ? 116 : 198], [() => c('sardina'), 208], [() => c('aceituna', 'alcaparra'), 115],
      [() => c('plátano'), 122], [() => c('guineo', 'banano'), 89], [() => c('aguacate'), 160], [() => c('yuca'), 159], [() => c('papa'), 77], [() => c('batata'), 86], [() => c('yautía', 'ñame'), 118],
      [() => c('auyama'), 26], [() => c('tomate'), 18], [() => c('cebolla', 'ají', 'pimiento'), 32], [() => c('ajo '), 149], [() => c('lechuga', 'espinaca', 'pepino', 'apio'), 16],
      [() => c('brócoli', 'coliflor', 'repollo'), 34], [() => c('zanahoria', 'remolacha'), 41], [() => c('manzana', 'pera'), 52], [() => c('fresa', 'melón', 'sandía', 'lechosa', 'piña'), 38],
      [() => c('uva', 'mango', 'chinola'), 65], [() => c('limón', 'naranja', 'mandarina'), 43],
      [() => c('pechuga de pollo', 'filete de pechuga'), 165], [() => c('pollo', 'muslo', 'alitas'), 215], [() => c('pavo') && !c('jamón'), 155],
      [() => c('churrasco', 'ribeye', 'angus', 'new york', 'filete de res'), 252], [() => c('carne molida', 'res '), 218], [() => c('chuleta', 'cerdo', 'costilla'), 242], [() => c('longaniza', 'chorizo'), 310],
      [() => c('salmón'), 208], [() => c('filete de mero', 'tilapia', 'bacalao', 'pescado'), 128], [() => c('camarón', 'camarones', 'marisco'), 99],
      [() => c('salami'), 305], [() => c('jamón de pavo', 'pechuga de pavo'), 108], [() => c('jamón serrano', 'prosciutto'), 240], [() => c('jamón'), 145], [() => c('tocineta', 'bacon'), 418], [() => c('salchicha'), 280],
      [() => c('huevo'), 155], [() => c('leche descremada', '0%'), 35], [() => c('leche de almendra'), 24], [() => c('leche de avena'), 46], [() => c('leche evaporada'), 134], [() => c('leche condensada'), 321], [() => c('leche'), 61],
      [() => c('yogurt griego'), 97], [() => c('yogurt'), 78], [() => c('queso mozzarella', 'queso de hoja'), 280], [() => c('queso cheddar', 'queso gouda', 'queso danés', 'queso parmesano'), 365],
      [() => c('queso crema'), 342], [() => c('queso blanco', 'queso fresco'), 264], [() => c('crema de leche'), 295],
      [() => c('pan integral'), 247], [() => c('pan de agua', 'pan sobao', 'baguette', 'pan de molde', 'pan '), 268], [() => c('tortilla', 'casabe'), 295],
      [() => c('croissant', 'bizcocho', 'pastelito', 'muffin', 'dona'), 385], [() => c('galleta', 'cereal', 'granola'), 435], [() => c('papas fritas', 'platanitos', 'nachos', 'doritos', 'snacks'), 512],
      [() => c('maní', 'almendra', 'nueces', 'pistacho'), 575], [() => c('chocolate', 'nutella'), 538], [() => c('helado'), 207], [() => c('pizza'), 266],
      [() => c('empanada', 'quipe', 'croqueta', 'tequeño'), 290], [() => c('papas congeladas', 'nuggets'), 275],
      [() => c('agua ', 'agua mineral', 'agua alcalina'), 0], [() => c('café') && !c('helado'), 2], [() => c('té verde', 'manzanilla', 'infusión'), 1],
      [() => c('refresco') && c('zero', 'light', 'sin azúcar'), 0], [() => c('refresco', 'cola', 'gaseosa', 'malta'), 42], [() => c('jugo', 'néctar'), 46], [() => c('bebida isotónica', 'energizante'), 28],
      [() => c('cerveza'), 43], [() => c('vino', 'sangría', 'espumante', 'prosecco'), 83], [() => c('ron ', 'whisky', 'vodka', 'tequila', 'ginebra'), 231]
    ];
    let kcal;
    const hit = T.find(([f]) => f());
    if (hit) kcal = typeof hit[1] === 'function' ? hit[1]() : hit[1];
    else kcal = { frutas: 48, carnes: 210, lacteos: 165, panaderia: 285, embutidos: 240, bebidas: 38, licores: 125, congelados: 235, snacks: 445 }[cat] ?? 190;
    const portion = ['bebidas', 'licores'].includes(cat) || c('leche', 'agua', 'jugo', 'aceite', 'vinagre') ? 'por cada 100 ml'
      : c('huevo') ? 'por cada 100 g (~2 huevos)' : 'por cada 100 g';
    const ingredients = cat === 'frutas' ? `100% ${before(before(before(p.name, '1'), 'Lb'), 'Und').trim()} natural fresco de cosecha seleccionada, sin aditivos ni conservantes.`
      : c('huevo') ? '100% huevos frescos de gallina grado A producidos en granjas locales certificadas.'
      : c('arroz') ? '100% arroz grano largo selecto enriquecido con hierro, niacina (B3), tiamina (B1) y ácido fólico.'
      : c('habichuela', 'guandul', 'garbanzo', 'lenteja') ? (c('lata', 'goya', 'linda') ? 'Granos seleccionados, agua purificada y sal yodada.' : '100% leguminosa seca natural seleccionada de cosecha fresca.')
      : c('aceite de oliva') ? '100% aceite de oliva virgen extra obtenido de primera presión en frío.'
      : c('aceite') ? 'Aceite vegetal 100% puro refinado (soya/girasol), sin colesterol y con vitamina E antioxidante.'
      : c('salami') ? 'Carne de cerdo y res seleccionada, especias dominicanas naturales (orégano, ajo, pimienta), sal curada, proteína de soya y humo natural.'
      : c('jamón') ? 'Pierna de cerdo o pechuga de pavo seleccionada, agua, sal marina, dextrosa, especias naturales y antioxidante eritorbato de sodio.'
      : c('queso') ? 'Leche fresca pasteurizada de vaca, cultivos lácticos seleccionados, cuajo microbiano y sal marina.'
      : c('leche de almendra') ? 'Agua filtrada, almendras seleccionadas (2.5%), carbonato de calcio, vitaminas A, D2, E y sal marina.'
      : c('leche') ? 'Leche fresca de vaca 100% grado A homogeneizada, ultrapasteurizada (UHT) y fortificada con vitaminas A y D3.'
      : c('yogurt') ? 'Leche fresca pasteurizada, cultivos probióticos vivos (L. bulgaricus, S. thermophilus, Bifidobacterium) y fermentos lácticos naturales.'
      : c('pan ', 'baguette', 'croissant') ? 'Harina de trigo enriquecida, agua purificada, levadura fresca, aceite vegetal/mantequilla, sal marina y malta de cebada.'
      : c('casabe') ? '100% yuca amarga dominicana rallada y tostada artesanalmente en plancha.'
      : c('espagueti', 'pasta ', 'coditos', 'fideos', 'lasaña') ? '100% sémola de trigo duro (Durum) enriquecida con hierro y vitaminas del complejo B.'
      : c('atún', 'sardina') ? 'Lomos de pescado seleccionados, agua o aceite vegetal puro y sal marina.'
      : cat === 'carnes' ? `Corte fresco 100% natural de ${before(before(p.name, '1'), 'Lb').trim()} refrigerado entre 0°C y 4°C sin hormonas añadidas.`
      : cat === 'bebidas' && c('agua') ? 'Agua purificada por ósmosis inversa y ozonizada con minerales equilibrados.'
      : cat === 'bebidas' && c('café') ? '100% granos de café arábica dominicano tostado y molido en su punto óptimo.'
      : cat === 'bebidas' ? 'Agua purificada, extractos/jugos naturales de fruta, regulador de acidez (ácido cítrico) y vitamina C.'
      : cat === 'licores' ? 'Destilado / fermentado original envejecido y embotellado en origen con control de calidad certificado.'
      : 'Ingredientes seleccionados bajo estándar de calidad Supermercados Bravo, libres de grasas trans artificiales.';
    const al = [];
    if (cat === 'lacteos' || c('leche', 'queso', 'yogurt', 'mantequilla', 'helado', 'crema de leche', 'chocolate con leche', 'croissant', 'pizza', 'tequeño')) {
      if (!c('almendra', 'avena', 'deslactosada', 'sin lactosa')) al.push('Lactosa / Lácteos');
      else if (c('deslactosada', 'sin lactosa')) al.push('Proteína de leche (Sin Lactosa)');
    }
    if (c('pan ', 'baguette', 'croissant', 'galleta', 'pasta ', 'espagueti', 'coditos', 'fideos', 'lasaña', 'harina de trigo', 'cerveza', 'malta', 'empanada', 'quipe', 'croqueta', 'tequeño', 'pizza', 'nuggets', 'avena')) {
      if (!c('sin gluten', 'casabe', 'maíz')) al.push('Gluten (Trigo / Cebada)');
    }
    if (c('huevo', 'mayonesa', 'bizcocho', 'muffin', 'dona')) al.push('Huevo');
    if (c('maní', 'almendra', 'nueces', 'pistacho', 'avellana', 'nutella')) al.push('Maní / Frutos Secos');
    if (c('atún', 'sardina', 'salmón', 'mero', 'tilapia', 'bacalao', 'pescado', 'camarón', 'camarones', 'marisco')) al.push('Pescado / Mariscos');
    if (c('soya', 'salami', 'salchicha')) al.push('Soya');
    if (c('vino', 'sangría', 'espumante')) al.push('Sulfitos');
    if (!al.length) al.push('Libre de alérgenos comunes');
    const diets = [];
    const hasGluten = al.some(a => a.includes('Gluten')), hasLactose = al.some(a => a.includes('Lactosa') && !a.includes('Sin Lactosa'));
    const animal = ['carnes', 'embutidos', 'lacteos'].includes(cat) || c('huevo', 'miel', 'atún', 'sardina', 'pollo', 'res', 'cerdo', 'mayonesa');
    if (!hasGluten) diets.push('Sin Gluten');
    if (!hasLactose) diets.push('Sin Lactosa');
    if (!animal) diets.push('Vegano / Plant-Based');
    if (cat === 'carnes' || c('huevo', 'atún', 'salmón', 'yogurt griego', 'queso', 'pechuga', 'jamón de pavo')) diets.push('Alto en Proteína');
    if (cat === 'carnes' || c('huevo', 'aguacate', 'aceite de oliva', 'queso', 'salmón', 'lechuga', 'espinaca', 'brócoli')) diets.push('Keto / Low-Carb');
    if (cat === 'frutas' || c('agua', 'avena', 'integral', 'zero', 'sin azúcar', 'almendra', 'yogurt griego', 'pechuga')) diets.push('Apto Diabéticos / Bajo Índice Glucémico');
    if (cat === 'frutas' || c('agua', 'arroz', 'avena', 'huevo')) diets.push('Bajo en Sodio');
    const [prot, carbs, fat] = cat === 'carnes' || c('atún', 'salmón') ? ['23.5 g', '0.0 g', '11.2 g']
      : cat === 'frutas' ? ['1.1 g', '11.5 g', '0.3 g'] : cat === 'lacteos' ? ['8.2 g', '4.8 g', '9.5 g']
      : cat === 'embutidos' ? ['16.4 g', '2.1 g', '18.0 g'] : cat === 'panaderia' ? ['8.5 g', '49.0 g', '4.2 g']
      : cat === 'bebidas' && kcal === 0 ? ['0.0 g', '0.0 g', '0.0 g'] : cat === 'bebidas' ? ['0.2 g', '10.4 g', '0.0 g'] : ['5.8 g', '28.4 g', '6.2 g'];
    return { kcal, portion, prot, carbs, fat, ingredients, allergens: al, diets };
  }

  window.BV_INFO = { describe, nutrition };
})();
