# DatConCli

## Objetivo do Projeto

Para realizar a conversão dos logs de voo (arquivos.DAT), é necessário utilizar o software ([DatCon](https://datfile.net/DatCon/intro.html)). No entando, o software perminte a conversão de apenas um arquivo por vez. Para automatizar a tarefa e converter grande volume de arquivos em um só comando, busca-se modificar o programa, para que, com uma linha de comando, sejam convertidos todos os arquivos dentro de uma pasta desejada.

A partir do repositório de referência clonado:

https://github.com/winstona/DatCon-1

- Compilar e executar o projeto
- Criar o executável .jar para release
- Adaptar e tratar o programa para converter todos os .DAT de determinada pasta e subpastas de uma vez, gerando os respectivos .csv
- Executar o script em python (embedded) que gera o csv  processado de todos os logs em um único arquivo, com os seguintes dados:
   - Porcentagem de bateria
   - Timestamp
   - Tempo de voo


## Instalação e Dependências

O projeto necessita do Java 8 (JDK 8 para desenvolver e compilar ou JRE 8 para executar o .jar independentemente)

Após fazer o clone do projeto, importe-o em alguma IDE e compile. Pela IDE gere o executável .jar

## Utilização

A execução é por linha de comando, e pode-se converter todos os logs .DAT de uma pasta e também das suas subpastas.

### Comando sugerido:

```bash
java -jar <path to DatConCli.jar> -i <path to .DAT or dir> -= -runscript
```
  
Este comando executa o programa DatConCli.jar, com os seguintes parâmetros:

```txt
-i => log específico ou pasta com os logs
-= => salva os arquivos convertidos na mesma pasta do arquivo original
-runscript =>executa o script em python que processa os arquivos .csv (este deve ser colocado na mesma pasta do input -i)
```

### Outros parâmetros
```txt
-o  <path>  => especifica pasta para o output
-s  <valor> => sample rate dos ticks dos .DAT (default está como 20 Hz, este aumenta a quantidade de linhas, original é 600 Hz)
-h => help
```
