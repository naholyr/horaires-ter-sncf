<?php

if (isset($_GET['source']) && $_GET['source'] == 1) { highlight_file(__FILE__); exit(0); }

// Libraries
require_once 'lib/common.inc.php';

// Configuration
$nb_max_departs = 20;
$nb_min_departs = 4;
$cache_timeout = 300;
$cache_enabled = !DEBUG;

// Paramètre "gare"
if (!isset($_GET['gare'])) {
  erreur('Parametre "gare" attendu', 310);
} else {
  $nom_gare = $_GET['gare'];
}
// Paramètre "id"
if (isset($_GET['id'])) {
  $id_gare = intval($_GET['id']);
} else {
  $id_gare = null;
}
// Paramètre "nb"
$nb_departs = $nb_max_departs;
if (isset($_GET['nb'])) {
  $nb_departs = intval($_GET['nb']);
}
$nb_departs = min(20, max(5, $nb_departs));

// Test cache
$key = 'gare-'.$nb_departs.'-'.str_replace(array("/", "\\", " ", "'"), array('', '', '', ''), $nom_gare) . '-ID-' . $id_gare;
if ($cache_enabled) {
  cache($key, $cache_timeout);
}

// Instance du client
$client = get_client('termobile_plus_gem');

// Recherche de la gare
$gares = $client->cherche_gare($nom_gare, $id_gare);
if (count($gares) > 1) {
  retour(array('gares' => $gares), 201);
}

// Demander les prochains départs pour cette gare
$departs = $client->cherche_departs($nom_gare, $nb_departs);
retour(array('nom' => $nom_gare, 'id' => $client->id_gare($nom_gare), 'departs' => $departs), 202);
