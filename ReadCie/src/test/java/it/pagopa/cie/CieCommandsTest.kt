package it.pagopa.cie

import it.pagopa.cie.cie.ApduManager
import it.pagopa.cie.cie.ApduResponse
import it.pagopa.cie.cie.NfcError
import it.pagopa.cie.cie.NfcEvent
import it.pagopa.cie.cie.OnTransmit
import it.pagopa.cie.cie.ReadCie
import it.pagopa.cie.nfc.BaseNfcImpl
import it.pagopa.cie.nfc.NfcReading
import it.pagopa.cie.nfc.Utils
import org.junit.Test

class CieCommandsTest {
    private val onTransmitGetRespTest = object : OnTransmit {
        override fun sendCommand(apdu: ByteArray, nfcEvents: NfcEvent): ApduResponse {
            return ApduResponse(Utils.hexStringToByteArray(""), Utils.hexStringToByteArray("9000"))
        }

        override fun error(error: NfcError) {
        }
    }
    private val onTransmitForException = object : OnTransmit {
        override fun sendCommand(apdu: ByteArray, nfcEvents: NfcEvent): ApduResponse {
            return ApduResponse(Utils.hexStringToByteArray(""))
        }

        override fun error(error: NfcError) {
        }
    }
    private val onTransmitApduTest = object : OnTransmit {
        override fun sendCommand(apdu: ByteArray,nfcEvents: NfcEvent): ApduResponse {
            return if (nfcEvents == NfcEvent.INTERNAL_AUTHENTICATION)
                ApduResponse(
                    Utils.hexStringToByteArray("back_from_test"),
                    Utils.hexStringToByteArray("9000")
                )
            else
                ApduResponse(Utils.hexStringToByteArray(""), Utils.hexStringToByteArray("2456"))
        }

        override fun error(error: NfcError) {
        }
    }

    private val onTransmit = object : OnTransmit {
        override fun sendCommand(apdu: ByteArray, nfcEvents: NfcEvent): ApduResponse {
            return when (Utils.bytesToString(apdu)) {
                "00A4040C0DA0000000308000000009816001", "00A4040C06A00000000039" -> ApduResponse(
                    Utils.hexStringToByteArray(""),
                    Utils.hexStringToByteArray("9000")
                )

                "00B081000C" -> ApduResponse(
                    Utils.hexStringToByteArray("393843139389135159633972"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B0850000" -> ApduResponse(
                    Utils.hexStringToByteArray("3082010A0282010100BCB2C666F4E163961D1BD44CCB51FAC61BFB6FA5957767650673101A483F3757AFGDB4DEAB0DEBD61A94631F455EFA37C12AE539B5553704CB45BEF2531804A1CEB304E3C9331C64039BC8D4D4C7623B6281B517E7EFC7E11A4A84F434D4F06653583D6E82309A681088DBD06016E4254B01CD869D4D39A74E8505097B80EB61E02C979A283CD16761682515B8728B076D79AA2A42A21484ED4B17AF9D92438244C615E1A80BCF52B70AAC6FA7B039ABCD363F83F9156282CBF591EE1CA33B5DD2A10AB40C0B9E367CD5291CBF18A6BD529D635CF468BB0ACFA15431DF73"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B085E700" -> ApduResponse(
                    Utils.hexStringToByteArray("2BB77450488BC655539155CA7031531FA27816731EB662EA4A4C3A3C2D6CC3E41DF10203010001"),
                    Utils.hexStringToByteArray("9000")
                )

                "0000000008DF9FEFECFAEFDFFE" -> ApduResponse(
                    Utils.hexStringToByteArray(""),
                    Utils.hexStringToByteArray("6D00")
                )

                "0022410006800102840100" -> ApduResponse(
                    Utils.hexStringToByteArray(""),
                    Utils.hexStringToByteArray("6A81")
                )

                "00B100060454020000E7" -> ApduResponse(
                    Utils.hexStringToByteArray("5381E477820B2030820B1C06092A864886F70D010702A0820B0D30820B09020103310D300B0609608648016503040203308201E20606678108010101A08201D6048201D2308201CE020101300B0609608648016503040203308201AA30450201A50440C831B743B4DD7174E575B8F7D01F16FB83D7B369686AAA39CEF3F7A6C4CE5A26B02E9C0D728E0C04247A87191F68212A103B428BAF07B459D7F074CD3897A068304502011B0440ED082BBBC8A9F758B59509048A8A95B1D5A44AA68D4EA13C140228F5F6636D39B79096C42F8E05A038000032DBA435B7D2448677CD0B377D26C0746721"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B1000604540200E4E7" -> ApduResponse(
                    Utils.hexStringToByteArray("5381E4F0102330450201A404409217C67362520F0DF0A3ACE62E652BB084719B323BF2F279858EAE18CCEEDEEFF2067478D8E76DC68C8EA2F0950FBF8FBB64F2175383603602F8229736BEFA5E30450201A304408866D859EE550B7D16D1FE879833848C0269D392B2DF250D26A716B1B0C96898ED9D1DA6AB0A560BA0D1CEECFD97039F5D2DB2D7944DF5B1BE284C40A5E7EAEA30450201A2044068AEA4E46C79E27637F43A90778F8422D3BE3FB827899D80EE90F78F73AB83AB391D8D1025D593AB848076CF003B174380D53B4EFBB7CE9EA8DF75AA946FE28B30450201A10440DFAD11C151"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B1000604540201C8E7" -> ApduResponse(
                    Utils.hexStringToByteArray("5381E4DDEAD39EF9DBB3D34EE58CDCFB9F7EC728423B6F467E58955C131B5E35A551EEC35A6C1F5D850E20082CB7772B4F235F14D97592D934C5FAB764A1300E1304303130381306303430303030A08206A4308206A030820454A00302010202084EE4231D2FCC5A3C304106092A864886F70D01010A3034A00F300D06096086480165030402030500A11C301A06092A864886F70D010108300D06096086480165030402030500A2030201403081903122302006035504030C194974616C69616E20436F756E747279205369676E6572204341313E303C060355040B0C354E6174696F6E616C20"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B1000604540202ACE7" -> ApduResponse(
                    Utils.hexStringToByteArray("5381E4456C656374726F6E69632043656E746572206F66204974616C69616E204E6174696F6E616C20506F6C696365311D301B060355040A0C144D696E6973747279206F6620496E746572696F72310B3009060355040613024954301E170D3232313030363039303035395A170D3334303130363039303035385A308193311C301A06035504030C13654964656E74697479436172645369676E6572310E300C06035504051305303030313231373035060355040B0C2E446972657A2E2043656E74722E2070657220692053657276697A692044656D6F67726166696369202D20434E5344311D"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B100060454020390E7" -> ApduResponse(
                    Utils.hexStringToByteArray("5381E4301B060355040A0C144D696E6973747279206F6620496E746572696F72310B300906035504061302495430820122300D06092A864886F70D01010105000382010F003082010A0282010100DD805992EA412AF1E76189BD48E76C235BBBFFDFF379D35EE6788325A21F0EDEBBCAE03F702B8F1E71AE41730316A4E155F0063D5C590D95F5A7B49FF4B92DD72A2D1EB83DAA8A1A9868972C469418F0298B05DED13A0096F2980480621B75AABD5BD030897A05FD361064A10F2929F183D37999F518BC81522DB69955DA1E729A7F4106306A9695042F228D2D77897938CCFE11FA65C0B096"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B100060454020474E7" -> ApduResponse(
                    Utils.hexStringToByteArray("5381E42EF6309798FC1C80ADA9A7FB50E858ADB905481AB2FD982BFE820F493354C69F12B1B807A739047B3E21EB684E70233772EABD449A34E25E28C2B7C89AC88E379BE4A2806B29613E88767E377073ED665D8E1756183A3828F229C996D6E59FC4516D93F525D27F0203010001A382018F3082018B3015060767810801010602040A30080201003103130143301F0603551D23041830168014D11A505E15ADEA5A61779CA4A2A991EC3949D1F930520603551D12044B3049811373706F632D69746140696E7465726E6F2E6974A410300E310C300A06035504070C03495441862068747470"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B100060454020558E7" -> ApduResponse(
                    Utils.hexStringToByteArray("5381E4733A2F2F637363612D6974612E696E7465726E6F2E676F762E69742F30520603551D11044B3049811373706F632D69746140696E7465726E6F2E6974A410300E310C300A06035504070C03495441862068747470733A2F2F637363612D6974612E696E7465726E6F2E676F762E69742F304D0603551D1F044630443042A040A03E863C68747470733A2F2F637363612D6974612E696E7465726E6F2E676F762E69742F6365727469666963617469435343412F43524C5F435343412E63726C301D0603551D0E041604149D37EE1076C12C9E296D6CA51ECAD1CA2AD9F179302B0603551D"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B10006045402063CE7" -> ApduResponse(
                    Utils.hexStringToByteArray("5381E41004243022800F32303232313030363039303035395A810F32303233303130343039303035395A300E0603551D0F0101FF040403020780304106092A864886F70D01010A3034A00F300D06096086480165030402030500A11C301A06092A864886F70D010108300D06096086480165030402030500A20302014003820201005E9D480558140A27B54693EA5ED044224D40D23A1DA826C801E63F6D0AD6EC046FCEC4ADE30C85BF5A2A97E11D7F8336C95E0A0CC638A7F2DA38B73E36A91915C14E032BABFFA990569C5BE529B99AD7DB7CF8E3F3A9DC74F30AA1FAA182265B90BA47FDE6"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B100060454020720E7" -> ApduResponse(
                    Utils.hexStringToByteArray("5381E4928F5E2714C4A6C944979A92A908C6E99B084535B440C983CB2C6A7D69660F8A8448420D8E1E97FD9DE30F2F467EB05987CC127C58511E96A356B98C06880A82C4EE70C6C1A7F1CB47D9FA6B9EF7DC9006D43FA2EC74035CAA20D76CA8A1B6F89509CDAD9460B3A55A92446C2FCF8F1C0EA817B0D39DC83D8DB10B7CCB447CE58DE84521DEA478E6DB8CF292D77DBD9B1EBD2768FD95125FD57FA3571F97E2EECED90E02A9AD41E275740BF42BEE36DC9D041A67C6FF729EF8E1EC231F57B6FD7269AF03285AC48096FCA49EE8B255E64F52F90F237FD862B98807BFF7D688AD7D0D95ED"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B100060454020804E7" -> ApduResponse(
                    Utils.hexStringToByteArray("5381E4222F3D0D90E8B3154EF1420C1B3BDE14431208DD8D12B44EBF13AAD4457D0EA88263F5B1290CB9C1B94A95FAAE09698F9811D6DFB97109529D5F11EBF9C4F099C5663E83D7905103CA5DBEC3BA64A029E1AE6CE9F308559F261A552AF880250AC2F42A46F225F87FA1E2381DFD0485D65DB56CE8B054B53454CFDE22E55921F10DE804586EDBCC6D8C8C5497FDA2D53571B70B29AC44D8029EE3A43559F803708C0D4F36EF1BB981B695BB8D41B142DED174F71BD47030318202653082026102010130819D3081903122302006035504030C194974616C69616E20436F756E7472792053"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B1000604540208E8E7" -> ApduResponse(
                    Utils.hexStringToByteArray("5381E469676E6572204341313E303C060355040B0C354E6174696F6E616C20456C656374726F6E69632043656E746572206F66204974616C69616E204E6174696F6E616C20506F6C696365311D301B060355040A0C144D696E6973747279206F6620496E746572696F72310B300906035504061302495402084EE4231D2FCC5A3C300B0609608648016503040203A068301506092A864886F70D01090331080606678108010101304F06092A864886F70D010904314204408653EA012B0223454BAFA6863E2E252ADB046EEBFD546CB52AA7D07339770314884BCB5C06F52675590A6D7BF6907C"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B1000604540209CCE7" -> ApduResponse(
                    Utils.hexStringToByteArray("5381E4047447CEED69C6DD024377B4DE96E1B5AE304106092A864886F70D01010A3034A00F300D06096086480165030402030500A11C301A06092A864886F70D010108300D06096086480165030402030500A203020140048201000035AAB8C24D829C3A2D0EFBE64EA25AF4796C4F25BDBA4F3EC75841F8059FF852516D6675ABA2F93A776FEFF43523FC8B1A201B440B41478C7F65A8911DB6AAE99C58999FAB2CB13AD73BED7A97726F9C0EB755CB97799864F459E8EE3AF82C710D8FAAFF2C8A2BF8310FD890115148120D857BE2F879F1F966782B858A19A1918D3D7ED5770270521BE07F"),
                    Utils.hexStringToByteArray("9000")
                )

                "00B100060454020AB0E7" -> ApduResponse(
                    Utils.hexStringToByteArray("5374C56C119E33EC02070124BD04B8B7C9EE675E047713DCBCD5DD71430F1056A213DC9318BF7F45F066FAD9A552D18D234E84329C8872F6AA24689370B26A462335D7BAF670DC8A15EE259E2FE70D5D80CF5312E7A5707B7F7DD27C47A3D58119B77D9C529C8C08630AE9FDBBA8E70CDF19590C1FCA"),
                    Utils.hexStringToByteArray("6282")
                )

                else -> ApduResponse(Utils.hexStringToByteArray("4th Response"))
            }
        }


        override fun error(error: NfcError) {
        }
    }

    @Test
    fun open_cie_test() {
        MyNfcImpl(object : NfcReading {
            override fun error(error: NfcError) {
            }
            override fun onTransmit(message: NfcEvent) {
            }

            override fun <T> read(element: T) {
            }
        }, onTransmit).transmit(10000, "ch"){

        }
    }

    @Test
    fun sendApduTest() {
        val commands = ApduManager(onTransmitApduTest)
        val list = ArrayList<Byte>()
        for (i in 0 until 256)
            list.add(if (i % 2 == 0) 0x00 else 0x02)
        val response = commands.sendApdu(
            byteArrayOf(0x00),
            list.toByteArray(),
            null,
            NfcEvent.INTERNAL_AUTHENTICATION
        )
        assert(Utils.bytesToString(response.response) == "BABFFFEFEFFEEF")
        assert(Utils.bytesToString(response.swByte) == "9000")
    }

    @Test
    fun sendApduDataEmptyTest() {
        val commands = ApduManager(onTransmitApduTest)
        val response =
            commands.sendApdu(byteArrayOf(0x00), byteArrayOf(), null, NfcEvent.INTERNAL_AUTHENTICATION)
        assert(Utils.bytesToString(response.swByte) == "9000")
    }

    @Test
    fun sendApduExceptionTest() {
        val commands = ApduManager(onTransmitApduTest)
        val list = ArrayList<Byte>()
        for (i in 0 until 256)
            list.add(if (i % 2 == 0) 0x00 else 0x02)
        try {
            commands.sendApdu(
                byteArrayOf(0x00),
                list.toByteArray(),
                null,
                NfcEvent.READ_FILE_SERVICE_ID_RESPONSE
            )
        } catch (e: Exception) {
            assert(e.message == "Errore apdu")
        }
    }

    @Test
    fun testException() {
        ReadCie(onTransmitForException, object : NfcReading {
            override fun onTransmit(message: NfcEvent) {
            }

            override fun <T> read(element: T) {
            }

            override fun error(error: NfcError) {
            }
        }).read("ch")
    }

    @Test
    fun test_apdu_resp_to_string() {
        val string = ApduResponse(Utils.hexStringToByteArray("test")).toString()
        assert(string == "ApduResponse: swHex:FEEF, swInt:-17")
    }

    @Test
    fun test_base_class() {
        MyNfcImpl(object : NfcReading {
            override fun onTransmit(message: NfcEvent) {
            }

            override fun <T> read(element: T) {
            }

            override fun error(error: NfcError) {
            }
        }, onTransmitForException).transmit(1000, "ch"){

        }
    }

    @Test
    fun get_resp_test() {
        val response = ApduManager(onTransmitGetRespTest).getResp(
            ApduResponse(Utils.hexStringToByteArray("test"), byteArrayOf(97.toByte(), 1.toByte())),
            NfcEvent.INTERNAL_AUTHENTICATION
        )
        assert(response.swHex == "9000")
        val response2 = ApduManager(onTransmitGetRespTest).getResp(
            ApduResponse(Utils.hexStringToByteArray("test"), byteArrayOf(97.toByte(), 0.toByte())),
            NfcEvent.INTERNAL_AUTHENTICATION
        )
        assert(response2.swHex == "9000")
    }

    internal class MyNfcImpl() : BaseNfcImpl() {

        private lateinit var onTransmit: OnTransmit

        constructor(readingInterface: NfcReading, onTransmit: OnTransmit) : this() {
            this.readingInterface = readingInterface
            this.onTransmit = onTransmit
        }

        override fun connect(
            isoDepTimeout: Int,
            onTagDiscovered: () -> Unit,
            actionDone: () -> Unit
        ) {
            actionDone.invoke()
        }

        override val readCie: ReadCie
            get() = ReadCie(this.onTransmit, this.readingInterface)

        override fun disconnect() {
        }
    }
}