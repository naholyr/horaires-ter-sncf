<?php

if (isset($_GET['source']) && $_GET['source'] == 1) { highlight_file(__FILE__); exit(0); }

// Configuration
$cache_timeout = 10000;
$cache_enabled = true;

// Libraries
require 'lib/common.inc.php';

// Paramètre "num"
$num = null;
if (!isset($_GET['num'])) {
  erreur('Parametre "num" attendu', 310);
} else {
  $num = $_GET['num'];
  if (!preg_match('/^[A-Za-z\-_0-9]+$/', $num)) {
    erreur('Parametre "num" invalide', 311);
  }
}

// Test cache
$key = 'train-'.$num;
if ($cache_enabled) {
  cache($key, $cache_timeout);
}

// Client TERMobile
$client = get_client('termobile');

// Recherche du train
$train = $client->cherche_train($num);
if (is_null($train)) {
  erreur('Train "' . $num . '" introuvable', 331);
}
retour($train);
