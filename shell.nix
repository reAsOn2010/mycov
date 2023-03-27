{ pkgs ? import <nixpkgs> {}}:
pkgs.mkShell {
  nativeBuildInputs = [
    pkgs.openjdk17-bootstrap
  ];
  shellHook = ''
    export JAVA_HOME=${pkgs.openjdk17-bootstrap}
  '';
}
