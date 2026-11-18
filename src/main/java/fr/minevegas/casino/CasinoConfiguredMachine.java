package fr.minevegas.casino;

public interface CasinoConfiguredMachine {
   CroupierSettings croupierSettings();

   void croupierSettings(CroupierSettings var1);

   CasinoMachineIdentity machineIdentity();

   void machineIdentity(CasinoMachineIdentity var1);

   default CasinoPlayerDisplay panelPlayer() {
      return CasinoPlayerDisplay.EMPTY;
   }
}
