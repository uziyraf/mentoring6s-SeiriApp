package com.example

import com.example.data.SampleData
import com.example.data.SeiriRepository
import com.example.data.service.ImportCsvService
import com.example.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {

    private lateinit var repository: SeiriRepository

    @Before
    fun setUp() {
        repository = SeiriRepository.instance
    }

    @Test
    fun testRepositoryInitialization() {
        assertNotNull(repository.currentUser.value)
        assertTrue(repository.objects.value.isNotEmpty())
        assertTrue(repository.areas.value.isNotEmpty())
        assertTrue(repository.zones.value.isNotEmpty())
        assertTrue(repository.questions.value.isNotEmpty())
    }

    @Test
    fun testSwitchDemoUser() {
        repository.switchDemoUser("usr_m1") // Andi Pratama
        assertEquals("Andi Pratama", repository.currentUser.value.name)
        assertEquals(UserRole.MENTOR, repository.currentUser.value.role)
        assertEquals("area_prf", repository.currentUser.value.assignedAreaId)

        // Switch back to Admin
        repository.switchDemoUser("usr_admin")
        assertEquals(UserRole.ADMIN, repository.currentUser.value.role)
    }

    @Test
    fun testAddObjectAndDuplicateDetection() {
        repository.switchDemoUser("usr_admin")
        val uniqueCode = "OBJ-TEST-999"

        val result1 = repository.addObject(
            code = uniqueCode,
            name = "Test Jig Alignment",
            areaId = "area_mc",
            zoneId = "zn_mc_1",
            description = "Test description",
            function = "Test function",
            category = "Jig",
            condition = ObjectCondition.BAIK,
            pic = "Test PIC",
            usageFrequency = UsageFrequency.DAILY
        )
        assertTrue(result1.isSuccess)

        // Attempt duplicate
        val result2 = repository.addObject(
            code = uniqueCode,
            name = "Duplicate Object",
            areaId = "area_mc",
            zoneId = "zn_mc_1",
            description = "Desc",
            function = "Func",
            category = "Jig",
            condition = ObjectCondition.BAIK,
            pic = "Test PIC",
            usageFrequency = UsageFrequency.DAILY
        )
        assertTrue(result2.isFailure)
    }

    @Test
    fun testTransferRequestAndAdminApproval() {
        // Mentor Andi (Preform) requests transfer to Warehouse (area_wh)
        repository.switchDemoUser("usr_m1")
        val targetObj = repository.objects.value.first { it.areaId == "area_prf" && it.isActive }

        val reqResult = repository.submitTransferRequest(
            objectId = targetObj.id,
            toAreaId = "area_wh",
            reason = "Dibutuhkan di Gudang",
            supportingNotes = "Unit lengkap"
        )
        assertTrue(reqResult.isSuccess)
        val request = reqResult.getOrThrow()
        assertEquals(TransferStatus.MENUNGGU_PERSETUJUAN, request.status)

        // Mentor cannot approve
        val mentorApproveAttempt = repository.reviewTransferRequest(request.id, true, "I approve myself")
        assertTrue(mentorApproveAttempt.isFailure)

        // Switch to Admin and approve
        repository.switchDemoUser("usr_admin")
        val adminApprove = repository.reviewTransferRequest(request.id, true, "Disetujui oleh Admin")
        assertTrue(adminApprove.isSuccess)

        // Object's area must now be area_wh
        val updatedObj = repository.objects.value.find { it.id == targetObj.id }
        assertNotNull(updatedObj)
        assertEquals("area_wh", updatedObj?.areaId)
    }

    @Test
    fun testCsvImportParser() {
        val sampleCsv = """
            Kode,Nama,Area,Zona,Kategori,Fungsi,PIC
            OBJ-NEW-99,"Kunci Sock 10mm",PRF-1,PRF-Z1,Perkakas,"Buka baut",Budi
            OBJ-ERR-01,"Tanpa Area",UNKNOWN,UNKNOWN,Alat,"Fungsi",Andi
        """.trimIndent()

        val preview = ImportCsvService.parseAndPreviewCsv(sampleCsv, repository.areas.value, repository.zones.value, repository.objects.value)
        assertEquals(2, preview.size)
        assertTrue(preview[0].isValid)
        assertFalse(preview[1].isValid)
    }
}
