//
//  ViewController.swift
//  CloudVision
//
//  Created by Breno Marques on 11/12/2017.
//  Copyright © 2017 Breno Marques. All rights reserved.
//

import UIKit

class MainViewController: UIViewController , UIImagePickerControllerDelegate , UINavigationControllerDelegate, UITableViewDelegate, UITableViewDataSource {
    let ImageFacesSize: CGFloat = 800
    
    @IBOutlet weak var tableCardsView: UITableView!
    let bmFacesDetector = BMFacesDetector()
    var crowds:[BMCrowd] = [BMCrowd]()
    
    override func viewDidLoad() {
        self.loadCrowds()
    }
    
    func doDetectFaces(_ imageFaces: UIImage!) {
        let resizedImage = BMImageUtilities.resizeImage(uiImage: imageFaces, newSize: ImageFacesSize)
        if ( self.bmFacesDetector.trackFaces(uiImage: resizedImage) ){
            guard let crowd = self.saveOneCrowd(self.bmFacesDetector) else {
                //TODO: incomplete code
                fatalError("One crowd was not saved with successful")
            }
            self.performSegue(
                withIdentifier: "SequeFacesViewController",
                sender: crowd
            )
        }
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        //TODO: incomplete code
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return crowds.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let bmCrowdCardView = tableView.dequeueReusableCell(withIdentifier: "BMCrowdCardView", for: indexPath) as? BMCrowdCardView  else {
            //TODO: incomplete code
            fatalError("The dequeued cell is not an instance of BMCrowdCardView.")
        }
        let bmCrowd = self.crowds[indexPath.item]
        bmCrowdCardView.BackgroundUIImage.image = bmCrowd.trackedUIImage
        bmCrowdCardView.titleLabel.text = bmCrowd.title
        return bmCrowdCardView
    }
    
    func tableView(_ tableView: UITableView, willSelectRowAt indexPath: IndexPath) -> IndexPath? {
        let crowd =  self.crowds[indexPath.item]
        performSegue(
            withIdentifier: "SequeFacesViewController",
            sender: crowd
        )
        return indexPath
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        super.prepare(for: segue, sender: sender)
        if ( segue.identifier == "SequeFacesViewController"){
            let facesViewController = segue.destination  as! FacesViewController
            facesViewController.bmCrowf = sender as! BMCrowd
        }
    }
    
    @IBAction func onTapPictureLibraryButton(_ sender: UIBarButtonItem) {
        let imagePicker =  BMImagePicker(delegate: self, sourceType: .photoLibrary)
        imagePicker.show()
    }
    
    @IBAction func onTapCameraButton(_ sender: UIBarButtonItem) {
        let imagePicker =  BMImagePicker(delegate: self, sourceType: .camera)
        imagePicker.show()
    }
    
    func imagePickerControllerDidCancel(_ picker: UIImagePickerController){
        dismiss(animated: true, completion: nil)
    }
    
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : Any]){
        guard let selectedImage = info[UIImagePickerControllerOriginalImage] as? UIImage else {
            //TODO: incomplete code
            fatalError("Expected a dictionary containing an image, but was provided the following: \(info)")
        }
        doDetectFaces(selectedImage)
        dismiss(animated: true, completion: nil)
    }
    
    func loadCrowds() {
        self.crowds = BMCrowd.load()
        self.tableCardsView.reloadData()
    }
    
    func saveOneCrowd(_ bmFacesDetector: BMFacesDetector! ) -> BMCrowd? {
        let created = Date()
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .full
        dateFormatter.timeStyle = .medium
        let people = bmFacesDetector.getFacesLocation().enumerated().map{
            (index, cgRect) in BMCrowd.Person(key: index, faceImageLocation: cgRect, winnerPosition: 0)
        }
        let bmCrowd = BMCrowd(
            title: dateFormatter.string(from: created),
            created: created,
            trackedUIImage: bmFacesDetector.trackedUIImage,
            people: people
        )
        self.crowds.insert(bmCrowd!, at:0 )
        if BMCrowd.save(crowds: self.crowds) {
            self.tableCardsView.reloadData()
            return bmCrowd
        }
        return nil
    }
}

