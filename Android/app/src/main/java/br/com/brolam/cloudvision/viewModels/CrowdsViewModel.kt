package br.com.brolam.cloudvision.viewModels

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.graphics.Bitmap
import android.os.AsyncTask
import android.text.format.DateUtils
import android.util.SparseArray
import br.com.brolam.cloudvision.R
import br.com.brolam.cloudvision.helpers.FacesDetector
import br.com.brolam.cloudvision.helpers.ImageUtil
import br.com.brolam.cloudvision.models.AppDatabase
import br.com.brolam.cloudvision.models.CrowdEntity
import br.com.brolam.cloudvision.models.CrowdPersonEntity
import com.google.android.gms.vision.face.Face
import java.util.*

/**
 * Created by brenomarques on 08/01/2018.
 *
 */
class CrowdsViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase: AppDatabase = AppDatabase.getInstance(application)
    private val crowdDao = this.appDatabase.crowdDao()
    private val imageUtil = ImageUtil(application)
    private var backgroundProcess: AsyncTask<Void, Void, Long>? = null

    fun hasBackgroundProcess():Boolean{
        return this.backgroundProcess?.status == AsyncTask.Status.RUNNING
    }

    fun getAllCrowds(): LiveData<List<CrowdEntity>> {
        return this.crowdDao.getAllOrderedByCreated()
    }

    fun insertCrowd(trackedImageBitmap: Bitmap, onCompleted: (Long) -> Unit, onError: (messageId: Int) -> Unit) {
        this.backgroundProcess = object : AsyncTask<Void, Void, Long>() {
            override fun doInBackground(vararg params: Void?): Long? {
                val facesDetector = FacesDetector(getApplication())
                facesDetector.trackFaces(trackedImageBitmap)
                if (facesDetector.trackingFaces.size() == 0) {
                    return null
                }
                val created = Date().time
                val title = DateUtils.formatDateTime(
                        getApplication(),
                        created,
                        DateUtils.FORMAT_SHOW_DATE + DateUtils.FORMAT_ABBREV_MONTH + DateUtils.FORMAT_SHOW_TIME)
                val trackingFaces = facesDetector.trackingFaces
                val trackedImageName = "/crowd_$created.jpg"
                return insertCrowdWithTransaction(title, trackedImageName, created, trackingFaces, trackedImageBitmap)

            }

            override fun onPostExecute(newCrowdId: Long?) {
                super.onPostExecute(newCrowdId)
                if (newCrowdId != null) {
                    onCompleted(newCrowdId)
                } else {
                    onError(R.string.exception_not_valid_picture)
                }
            }
        }
        this.backgroundProcess?.execute()
    }

    private fun insertCrowdWithTransaction(title: String, trackedImageName: String, created: Long, trackingFaces: SparseArray<Face>, trackedImageBitmap: Bitmap): Long {
        var crowdId: Long = -1
        this.appDatabase.runInTransaction {
            val crowdEntity = CrowdEntity(title = title, trackedImageName = trackedImageName, created = created)
            crowdId = this.crowdDao.insert(crowdEntity)
            val crowdPeople = mutableListOf<CrowdPersonEntity>()
            for (index in 0 until trackingFaces.size()) {
                val face = trackingFaces.valueAt(index)
                val crowdPerson = CrowdPersonEntity(
                        crowdId = crowdId,
                        insertedOrder = index,
                        faceWidth = face.width,
                        faceHeight = face.height,
                        facePositionX = face.position.x,
                        facePositionY = face.position.y)
                crowdPeople.add(crowdPerson)
                this.imageUtil.save(trackedImageName, trackedImageBitmap)
            }
            this.crowdDao.insert(crowdPeople)

        }
        return crowdId
    }

    fun deleteCrowd(crowd: CrowdEntity) {
        AsyncTask.execute {
            appDatabase.runInTransaction {
                crowdDao.deleteOneCrowd(crowd)
                imageUtil.delete(crowd.trackedImageName)
            }
        }
    }
}