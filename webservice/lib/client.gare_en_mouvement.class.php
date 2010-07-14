<?php

require_once dirname(__FILE__) . '/client.interface.php';

class client_gare_en_mouvement implements client_interface
{

  private static $ids = array (
  'abbeville' => 'ABB',
  'aeroport-cdg-2-tgv' => 'RYT',
  'agen' => 'AGN',
  'aix-en-provence' => 'AXP',
  'aix-en-provence-tgv' => 'AXV',
  'aix-les-bains-le-revard' => 'AIX',
  'albertville' => 'ALV',
  'amiens' => 'AMS',
  'angers-st-laud' => 'ASL',
  //'angouleme' => false,
  'annecy' => 'ANY',
  'antibes' => 'ATB',
  'arles' => 'ARS',
  'arras' => 'ARR',
  'auray' => 'ARY',
  'avignon-centre' => 'AVI',
  //'avignon-sud' => false,
  'avignon-tgv' => 'COU',
  'bar-le-duc' => 'BLD',
  'bayonne' => 'BYE',
  'belfort' => 'BFT',
  'bellegarde' => 'BGD',
  'besancon-viotte' => 'BNV',
  'bethune' => 'BET',
  'beziers' => 'BZS',
  //'biarritz' => false,
  'blois' => 'BLO',
  'bordeaux-st-jean' => 'BXJ',
  'boulogne-ville' => 'BEM',
  'bourg-en-bresse' => 'BGB',
  'bourges' => 'BGS',
  'bourg-st-maurice' => 'BSM',
  'brest' => 'BRT',
  'brive-la-gaillarde' => 'BLG',
  'caen' => 'CAE',
  'cahors' => 'CAH',
  'calais-frethun' => 'FHS',
  'calais-ville' => 'CSV',
  'cannes' => 'CAN',
  'carcassonne' => 'CNE',
  'chalons-en-champagne' => 'CMP',
  'chalon-sur-saone' => 'CSS',
  'chambery-challes-les-eaux' => 'CRL',
  'champagne-ardenne' => 'CGV',
  'charleville-mezieres' => 'CMZ',
  'chartres' => 'CHH',
  'chateauroux' => 'CTX',
  'chateau-thierry' => 'CTH',
  'chatellerault' => 'CRT',
  //'cherbourg' => false,
  'clermont-ferrand' => 'CLF',
  'colmar' => 'CMR',
  'compiegne' => 'CPE',
  //'creil' => false,
  'dax' => 'DAX',
  'dijon-ville' => 'DJV',
  'dole-ville' => 'DLE',
  'douai' => 'DOI',
  'dreux' => 'DRX',
  'dunkerque' => 'DKQ',
  'epernay' => 'EPR',
  'epinal' => 'ELP',
  'evreux-embranchement' => 'EVX',
  'forbach' => 'FOB',
  'futuroscope' => 'FTU',
  'granville' => 'GRV',
  'grenoble' => 'GRE',
  //'guingamp' => false,
  'hendaye' => 'HED',
  'la-baule-escoublac' => 'LBE',
  'la-rochelle-ville' => 'LRE',
  'laroche-migennes' => 'LAR',
  'la-roche-sur-yon' => 'LRY',
  'laval' => 'LAL',
  'le-creusot-tgv' => 'LCM',
  'le-havre' => 'LHA',
  'le-mans' => 'LEN',
  'lens' => 'LNS',
  'les-arcs-draguignan' => 'LAC',
  'les-aubrais-orleans' => 'LAB',
  'libourne' => 'LIB',
  'lille-europe' => 'LEW',
  'lille-flandres' => 'LLF',
  'limoges-benedictins' => 'LIM',
  'lisieux' => 'LIS',
  'longueau' => 'LUA',
  'lons-le-saunier' => 'LLS',
  'lorient' => 'LRT',
  'lorraine-tgv' => 'TGL',
  //'lourdes' => false,
  'lyon-part-dieu' => 'LYD',
  'lyon-perrache' => 'LPR',
  'lyon-st-exupery-tgv' => 'SXA',
  'macon-loche-tgv' => 'MLH',
  'macon-ville' => 'MAC',
  'marne-la-vallee-chessy' => 'MLV',
  'marseille-st-charles' => 'MSC',
  'massy-tgv' => 'MPW',
  'metz-ville' => 'MZE',
  'meuse-tgv' => 'TGM',
  'monaco-monte-carlo' => 'MNA',
  //'montargis' => false,
  'montauban-ville-bourbon' => 'MBN',
  'mont-de-marsan' => 'MMR',
  'montelimar' => 'MTR',
  'montlucon-ville' => 'MLN',
  'montpellier-st-roch' => 'MPL',
  //'morlaix' => false,
  'moulins-sur-allier' => 'MSA',
  'moutiers-salins-brides-les-bains' => 'MOS',
  'mulhouse-ville' => 'MSE',
  'nancy-ville' => 'NCY',
  'nantes' => 'NTS',
  'narbonne' => 'NBN',
  'nevers' => 'NVS',
  'nice-ville' => 'NIC',
  'nimes' => 'NMS',
  'niort' => 'NRT',
  'orleans' => 'ORL',
  //'orthez' => false,
  'paris-austerlitz' => 'PAZ',
  'paris-bercy' => 'PBY',
  'paris-est' => 'PES',
  'paris-gare-de-lyon' => 'PLY',
  'paris-montparnasse-1-et-2' => 'PMP',
  'paris-nord' => 'PNO',
  'paris-st-lazare' => 'PSL',
  'pau' => 'PAU',
  'perigueux' => 'PXR',
  'perpignan' => 'PPN',
  'poitiers' => 'PST',
  'quimper' => 'QPR',
  'redon' => 'RDN',
  'reims' => 'RMS',
  'rennes' => 'RES',
  //'rochefort' => false,
  //'roubaix' => false,
  'rouen-rive-droite' => 'RRD',
  'sete' => 'STE',
  'st-brieuc' => 'SBC',
  'st-etienne-chateaucreux' => 'SEN',
  'st-jean-de-luz-ciboure' => 'SJZ',
  'st-nazaire' => 'SNA',
  'st-pierre-des-corps' => 'SPC',
  'st-quentin' => 'SQT',
  'st-rapha"el-valescure' => 'SRV',
  'strasbourg' => 'STG',
  //'surgeres' => false,
  'tarbes' => 'TBS',
  'tgv-haute-picardie' => 'HPI',
  'thionville' => 'THL',
  'toulon' => 'TLN',
  'toulouse-matabiau' => 'TSE',
  'tourcoing' => 'TRG',
  'tours' => 'TRS',
  'trouville-deauville' => 'TDE',
  'troyes' => 'TOY',
  'val-de-reuil' => 'VDR',
  'valence-tgv' => 'VAL',
  'valence-ville-' => 'VCE',
  'valenciennes' => 'VAS',
  'vannes' => 'VAN',
  //'vendome-villiers-sur-loir' => false,
  'vernon' => 'VNO',
  //'versailles-chantiers' => false,
  'vesoul' => 'VES',
  'vichy' => 'VHY',
  'vierzon-ville' => 'VER',
  );

  public function __construct()
  {
  }

  public function cherche_gare(&$nom_gare, $id_gare)
  {
    $real_id = $this->id_gare($nom_gare);
    if (is_null($real_id)) {
      erreur('Gare "' .$nom_gare. '" inconnue pour Gares En Mouvement', 330);
    }
    if (!is_null($id_gare) && $real_id != $id_gare) {
      erreur('ID "' .$id_gare. '" invalide pour la gare "' .$nom_gare. '" (ID réel "' .$real_id. '"', 331);
    }
    return array(utf8_encode(nom_gare($nom_gare)) => $real_id);
  }

  public function cherche_departs($nom_gare, $nb_departs)
  {
    $departs = array();
    $id = $this->id_gare($nom_gare);
    $url = 'http://www.gares-en-mouvement.com/include/tvs.php?nom_gare=&tab=dep&TVS=http://www.gares-en-mouvement.com/tvs/TVS?wsdl&tab_summary_dep=&caption=&type=T&numero=N&num=N&heure=H&dest=D&origine=P&situation=S&voie=V&color=bleu&heur=h&h=h&minut=min&m=min&arriv=A&retard=RETARD&code_tvs=' . rawurlencode($id);
    if (DEBUG) var_dump($url);
    $html = file_get_contents($url);
    if (DEBUG) var_dump($html);
    if (preg_match('#<tbody>(.*?)</tbody>#si', $html, $tbody)) {
      $tbody = $tbody[1];
      preg_match_all('#<tr.*?>(.*?)</tr>#si', $tbody, $rows, PREG_SET_ORDER);
      foreach ($rows as $row) {
        $row = $row[1];
        preg_match_all('#<td.*?>(.*?)</td>#si', $row, $cells, PREG_SET_ORDER);
        $type = trim($cells[0][1]);
        $num = trim($cells[1][1]);
        if ($num == '' || $num == '&nbsp;') {
          continue;
        }
        $heure = trim(strip_tags($cells[2][1]));
        $dest = trim($cells[3][1]);
        $situation = trim(strip_tags($cells[4][1]));
        $voie = trim(strip_tags($cells[5][1]));
        $depart = array(
          'attention' => '',
          'type' => preg_match('#alt="(.*?)"#i', $type, $m) ? $m[1] : strip_tags($type),
          'numero' => $num,
          'heure' => $heure,
          'destination' => nom_gare($dest),
          'voie' => $voie == 'AN' ? '' : $voie,
          'supprime' => preg_match('#SUPPR#i', $situation) ? true : false,
          'retards' => array(),
          'source' => 'gare-en-mouvement',
        );
        if (preg_match('#RETARD *: *(.*?)$#i', $situation, $m)) {
          $depart['retards'][] = array('retard' => $m[1], 'motif' => '');
        }
        $departs[] = $depart;
      }
    }
    return $departs;
  }

  public function cherche_train($num)
  {
    return null;
  }

  public function id_gare($nom_gare)
  {
    $nom_gare = nom_gare($nom_gare);
    $nom_gare = strtr(utf8_decode($nom_gare), utf8_decode(
      'ÀÁÂÃÄÅÇÈÉÊËÌÍÎÏÒÓÔÕÖÙÚÛÜÝàáâãäåçèéêëìíîïðòóôõöùúûüýÿ'),
      'AAAAAACEEEEIIIIOOOOOUUUUYaaaaaaceeeeiiiioooooouuuuyy');
    $key = preg_replace('/[\'\^\`]([aeiou])/', '$1', preg_replace('/ +/', '-', strtolower($nom_gare)));
    if (isset(self::$ids[$key])) {
      return self::$ids[$key];
    } else {
      return null;
    }
  }

}